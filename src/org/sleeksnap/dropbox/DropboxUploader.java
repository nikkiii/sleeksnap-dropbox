package org.sleeksnap.dropbox;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

import org.json.JSONObject;
import org.sleeksnap.Constants.Application;
import org.sleeksnap.upload.FileUpload;
import org.sleeksnap.upload.ImageUpload;
import org.sleeksnap.upload.TextUpload;
import org.sleeksnap.uploaders.Settings;
import org.sleeksnap.uploaders.Uploader;
import org.sleeksnap.uploaders.UploaderConfigurationException;
import org.sleeksnap.uploaders.generic.GenericUploader;
import org.sleeksnap.uploaders.settings.ParametersDialog;
import org.sleeksnap.uploaders.settings.UploaderSettings;
import org.sleeksnap.util.Utils.FileUtils;

import com.dropbox.core.DbxClient;
import com.dropbox.core.DbxEntry;
import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.DbxWriteMode;

@Settings(required = { "account|dropboxoauth" }, optional = { })
public class DropboxUploader extends GenericUploader {
	
	public static DbxRequestConfig REQUEST_CONFIG = new DbxRequestConfig(Application.NAME, Locale.getDefault().toString());
	
	static {
		ParametersDialog.registerSettingType("dropboxoauth", new DropboxOAuthSettingType());
	}
	

	public static final String APP_KEY = "zssl93en2okobqz";
	public static final String APP_SECRET = "eb9rus8f2h9x1c6";

	private DbxClient client;

	/**
	 * The uploader array
	 */
	private Uploader<?>[] uploaders = new Uploader<?>[] { new DropboxImageUploader(), new DropboxTextUploader(), new DropboxFileUploader() };
	
	public void checkClient() {
		if(client == null) {
			client = new DbxClient(REQUEST_CONFIG, settings.getJSONObject("account").getString("accessToken"));
		}
	}
	
	public String dropboxUpload(String fileName, InputStream input) throws UploaderConfigurationException, IOException, DbxException {
		return dropboxUpload(fileName, input, input.available());
	}
	
	public String dropboxUpload(String fileName, InputStream input, int dataLength) throws IOException, DbxException, UploaderConfigurationException {
		if (!settings.has("account")) {
			throw new UploaderConfigurationException("Account not authorized");
		}
		
		checkClient();
		
		try {
			DbxEntry.File uploadedFile = client.uploadFile("/" + fileName, DbxWriteMode.add(), dataLength, input);
			
			return client.createShareableUrl(uploadedFile.path);
		} finally {
			input.close();
		}
	}

	/**
	 * An uploader to deal with Image uploads
	 * 
	 * @author Nikki
	 * 
	 */
	public class DropboxImageUploader extends Uploader<ImageUpload> {

		@Override
		public String getName() {
			return DropboxUploader.this.getName();
		}

		@Override
		public String upload(ImageUpload t) throws Exception {
			return dropboxUpload(FileUtils.generateFileName(t), t.asInputStream());
		}
	}

	/**
	 * An uploader to deal with Text uploads
	 * 
	 * @author Nikki
	 * 
	 */
	public class DropboxTextUploader extends Uploader<TextUpload> {

		@Override
		public String getName() {
			return DropboxUploader.this.getName();
		}

		@Override
		public String upload(TextUpload t) throws Exception {
			return dropboxUpload(FileUtils.generateFileName(t), t.asInputStream());
		}
	}

	/**
	 * An uploader to deal with File uploads
	 * 
	 * @author Nikki
	 * 
	 */
	public class DropboxFileUploader extends Uploader<FileUpload> {

		@Override
		public String getName() {
			return DropboxUploader.this.getName();
		}

		@Override
		public String upload(FileUpload t) throws Exception {
			return dropboxUpload(FileUtils.generateFileName(t), t.asInputStream(), (int) t.getFile().length());
		}
	}

	@Override
	public Uploader<?>[] getSubUploaders() {
		return uploaders;
	}

	@Override
	public String getName() {
		return "Dropbox";
	}

	@Override
	public boolean validateSettings(UploaderSettings settings) throws UploaderConfigurationException {
		if(!settings.has("account")) {
			return true;
		}
		
		JSONObject account = settings.getJSONObject("account");
		
		
		client = new DbxClient(REQUEST_CONFIG, account.getString("accessToken"));
		
		try {
			client.getAccountInfo();
		} catch (DbxException e) {
			throw new UploaderConfigurationException("Access token is invalid");
		}
		return true;
	}

}
