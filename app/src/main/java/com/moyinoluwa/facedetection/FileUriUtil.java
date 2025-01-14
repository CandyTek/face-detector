package com.moyinoluwa.facedetection;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.CursorLoader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;

import java.io.File;

/**
 * 文件路径 URI 类
 * 可以把别的软件 Provider 传过来的 uri 通过此类来进行解析
 */
public class FileUriUtil {

	public static String getRealPath(Context context,Uri fileUri) {
		String realPath;
		// SDK < API11
		if (Build.VERSION.SDK_INT < 19) {
			realPath = FileUriUtil.getRealPathFromURI_API11to18(context,fileUri);
		}
		// SDK > 19 (Android 4.4) and up
		else {
			realPath = FileUriUtil.getRealPathFromURI_API19(context,fileUri);
		}
		return realPath;
	}

	@SuppressLint("NewApi")
	public static String getRealPathFromURI_API11to18(Context context,Uri contentUri) {
		String[] proj = {MediaStore.Images.Media.DATA};
		String result = null;

		CursorLoader cursorLoader = new CursorLoader(context,contentUri,proj,null,null,null);
		Cursor cursor = cursorLoader.loadInBackground();

		if (cursor != null) {
			int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
			cursor.moveToFirst();
			result = cursor.getString(column_index);
			cursor.close();
		}
		return result;
	}

	public static String getRealPathFromURI_BelowAPI11(Context context,Uri contentUri) {
		String[] proj = {MediaStore.Images.Media.DATA};
		Cursor cursor = context.getContentResolver().query(contentUri,proj,null,null,null);
		int column_index;
		String result = "";
		if (cursor != null) {
			column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
			cursor.moveToFirst();
			result = cursor.getString(column_index);
			cursor.close();
			return result;
		}
		return result;
	}

	@SuppressLint("NewApi")
	public static String getRealPathFromURI_API19(final Context context,final Uri uri) {

		final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

		// DocumentProvider
		if (isKitKat && DocumentsContract.isDocumentUri(context,uri)) {
			// ExternalStorageProvider
			if (isExternalStorageDocument(uri)) {
				final String docId = DocumentsContract.getDocumentId(uri);
				final String[] split = docId.split(":");
				final String type = split[0];

				// This is for checking Main Memory
				if ("primary".equalsIgnoreCase(type)) {
					if (split.length > 1) {
						return Environment.getExternalStorageDirectory() + "/" + split[1];
					} else {
						return Environment.getExternalStorageDirectory() + "/";
					}
					// This is for checking SD Card
				} else {
					return "storage" + "/" + docId.replace(":","/");
				}

			}
			// DownloadsProvider
			else if (isDownloadsDocument(uri)) {
				String fileName = getFilePath(context,uri);
				if (fileName != null) {
					return Environment.getExternalStorageDirectory().toString() + "/Download/" + fileName;
				}

				String id = DocumentsContract.getDocumentId(uri);
				if (id.startsWith("raw:")) {
					id = id.replaceFirst("raw:","");
					File file = new File(id);
					if (file.exists())
						return id;
				}

				final Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"),Long.parseLong(id));
				return getDataColumn(context,contentUri,null,null);
			}
			// MediaProvider
			else if (isMediaDocument(uri)) {
				final String docId = DocumentsContract.getDocumentId(uri);
				final String[] split = docId.split(":");
				final String type = split[0];

				Uri contentUri = null;
				if ("image".equals(type)) {
					contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
				} else if ("video".equals(type)) {
					contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
				} else if ("audio".equals(type)) {
					contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
				}

				final String selection = "_id=?";
				final String[] selectionArgs = new String[]{
						split[1]
				};

				return getDataColumn(context,contentUri,selection,selectionArgs);
			}
		}
		// MediaStore (and general)
		else if ("content".equalsIgnoreCase(uri.getScheme())) {

			// Return the remote address
			if (isGooglePhotosUri(uri))
				return uri.getLastPathSegment();

			return getDataColumn(context,uri,null,null);
		}
		// File
		else if ("file".equalsIgnoreCase(uri.getScheme())) {
			return uri.getPath();
		}

		return null;
	}

	public static String getDataColumn(Context context,Uri uri,String selection,
									   String[] selectionArgs) {

		Cursor cursor = null;
		final String column = "_data";
		final String[] projection = {
				column
		};

		try {
			cursor = context.getContentResolver().query(uri,projection,selection,selectionArgs,
					null);
			if (cursor != null && cursor.moveToFirst()) {
				final int index = cursor.getColumnIndexOrThrow(column);
				return cursor.getString(index);
			}
		}
		finally {
			if (cursor != null)
				cursor.close();
		}
		return null;
	}

	public static String getFilePath(Context context,Uri uri) {

		final String[] projection = {
				MediaStore.MediaColumns.DISPLAY_NAME
		};
		try (Cursor cursor = context.getContentResolver().query(uri,projection,null,null,
				null)) {
			if (cursor != null && cursor.moveToFirst()) {
				final int index = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME);
				return cursor.getString(index);
			}
		}
		return null;
	}

	/** 返回 Uri的颁发机构是否为 ExternalStorageProvider */
	public static boolean isExternalStorageDocument(Uri uri) {
		return "com.android.externalstorage.documents".equals(uri.getAuthority());
	}

	/** 返回 Uri的颁发机构是否为 DownloadsProvider */
	public static boolean isDownloadsDocument(Uri uri) {
		return "com.android.providers.downloads.documents".equals(uri.getAuthority());
	}

	/** 返回 Uri的颁发机构是否为 MediaProvider */
	public static boolean isMediaDocument(Uri uri) {
		return "com.android.providers.media.documents".equals(uri.getAuthority());
	}

	/** 返回 Uri的颁发机构是否为 Google Photos */
	public static boolean isGooglePhotosUri(Uri uri) {
		return "com.google.android.apps.photos.content".equals(uri.getAuthority());
	}

	public static String getFileByUri(Context context,Uri uri) {
		String path;
		path = uri.getEncodedPath();
		if (path != null) {
			path = Uri.decode(path);
			ContentResolver cr = context.getContentResolver();
			Cursor cur = cr.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,new String[]{MediaStore.Images.ImageColumns._ID,MediaStore.Images.ImageColumns.DATA},"(" + MediaStore.Images.ImageColumns.DATA + "=" + "'" + path + "'" + ")",null,null);
			int index = 0;
			int dataIdx = 0;
			for (cur.moveToFirst();!cur.isAfterLast();cur.moveToNext()) {
				index = cur.getColumnIndex(MediaStore.Images.ImageColumns._ID);
				index = cur.getInt(index);
				dataIdx = cur.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
				path = cur.getString(dataIdx);
			}
			cur.close();
			if (index == 0) {
			} else {
				Uri u = Uri.parse("content://media/external/images/media/" + index);
				System.out.println("temp uri is :" + u);
			}
		}
		if (path != null) {
			return path;
		}

		return "";
	}
}

