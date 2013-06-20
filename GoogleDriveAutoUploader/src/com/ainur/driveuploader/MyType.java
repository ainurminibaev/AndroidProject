package com.ainur.driveuploader;

import com.google.api.client.http.FileContent;
import com.google.api.services.drive.model.File;

public class MyType {
	FileContent mediaContent;
	File body;

	public FileContent getMediaContent() {
		return mediaContent;
	}

	public File getBody() {
		return body;
	}

	public MyType(FileContent mediaContent, File body) {
		super();
		this.mediaContent = mediaContent;
		this.body = body;
	}
}
