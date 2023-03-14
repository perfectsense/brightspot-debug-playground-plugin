package com.brightspot.debug;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import com.brightspot.settings.DebugEnvironment;
import com.brightspot.settings.DebugEnvironmentSettingsState;
import com.intellij.ide.actions.OpenInRightSplitAction;
import com.intellij.ide.browsers.actions.WebPreviewVirtualFile;
import com.intellij.ide.scratch.ScratchFileService;
import com.intellij.ide.scratch.ScratchRootType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.psi.PsiFile;
import com.intellij.util.LocalFileUrl;
import com.twelvemonkeys.lang.StringUtil;
import icons.BrightspotIcons;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

public class DebugCodeAction extends AnAction {

    public static final String RESPONSE_DIR_NAME = "Responses";

    private final String environmentName;

    public DebugCodeAction(String environmentName) {
        super(environmentName);
        this.environmentName = environmentName;
        getTemplatePresentation().setIcon(BrightspotIcons.BrightspotOIcon);
    }

    @Override
    public void update(AnActionEvent e) {
        super.update(e);
        e.getPresentation().setEnabledAndVisible(true);
    }

    @Override
    public void actionPerformed(AnActionEvent e) {

        PsiFile psiFile = e.getRequiredData(CommonDataKeys.PSI_FILE);
        String name = psiFile.getName();
        //remove suffix
        if (name.contains(".")) {
            name = name.substring(0, name.lastIndexOf('.'));
        }
        String code = psiFile.getText();

        DebugEnvironment environment = null;
        for (DebugEnvironment env : DebugEnvironmentSettingsState.getInstance().getSavedFields()) {
            if (env.getName().equals(environmentName)) {
                environment = env;
            }
        }

        if (!code.isEmpty() && environment != null) {
            sendRequest(environment, name, code, e);
        }

    }

    public void sendRequest(DebugEnvironment environment, String name, String code, AnActionEvent e) {

        String url = environment.getUrl();
        String username = environment.getUsername();
        String creds = environment.getCreds();

        try {

            final HttpPost httpPost = new HttpPost(url);
            final List<NameValuePair> params = new ArrayList<NameValuePair>();
            params.add(new BasicNameValuePair("action", "run"));
            params.add(new BasicNameValuePair("type", "Java"));
            params.add(new BasicNameValuePair("code", code));
            httpPost.setEntity(new UrlEncodedFormEntity(params));

            httpPost.addHeader("Content-Type", " application/x-www-form-urlencoded");

            if (!creds.isEmpty()) {
                httpPost.addHeader("Authorization", "Basic " +
                    Base64.getEncoder().encodeToString((username + ":" + creds).getBytes()));
            }

            try (CloseableHttpClient client = HttpClients.createDefault();
                CloseableHttpResponse response = (CloseableHttpResponse) client
                    .execute(httpPost)) {

                HttpEntity entity = response.getEntity();
                String responseString = EntityUtils.toString(entity, "UTF-8");
                if (StringUtil.isEmpty(responseString)) {
                    //response string cannot be empty? maybe do something else here?
                    return;
                }

                Project project = e.getProject();
                if (project == null) {
                    return;
                }

                //get the path for scratch files
                ScratchFileService scratchFileService = ScratchFileService.getInstance();
                String scratchPath = scratchFileService.getRootPath(ScratchRootType.getInstance());
                VirtualFile scratchDirectory = VirtualFileManager.getInstance().findFileByUrl("file://" + scratchPath);
                if (scratchDirectory == null) {
                    return;
                }

                //get the response directory
                VirtualFile responseDirectory = scratchDirectory.findChild(RESPONSE_DIR_NAME);
                if (responseDirectory == null) {
                    responseDirectory = scratchDirectory.createChildDirectory(this, RESPONSE_DIR_NAME);
                }

                //add date, original file name, etc
                String responseFileName = name+"-response-"+LocalDateTime.now().toString() + ".html";

                //create the file
                AtomicReference<VirtualFile> file = new AtomicReference<>();
                VirtualFile finalResponseDirectory = responseDirectory;
                ApplicationManager.getApplication().runWriteAction(() -> {
                    try {
                        file.set(finalResponseDirectory.createChildData(this, responseFileName));
                        file.get().setBinaryContent(responseString.getBytes());
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                });

                if(file.get() == null) {
                    return;
                }

                //open the file in IntelliJ IDEA internal preview
                WebPreviewVirtualFile webPreviewVirtualFile = new WebPreviewVirtualFile(file.get(), new LocalFileUrl(
                    file.get().getUrl()));
                OpenInRightSplitAction.Companion.openInRightSplit(
                    project,
                    webPreviewVirtualFile,
                    null,
                    true
                );

            }

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

}
