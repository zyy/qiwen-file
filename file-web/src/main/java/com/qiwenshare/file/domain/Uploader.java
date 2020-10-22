package com.qiwenshare.file.domain;

import java.io.*;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import com.qiwenshare.file.util.ImageUtils;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.util.Streams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.support.StandardMultipartHttpServletRequest;

import com.qiwenshare.file.util.FileUtils;
import com.qiwenshare.file.util.PathUtils;

/**
 * 文件上传辅助类
 */
public class Uploader {
    private static final Logger LOG = LoggerFactory.getLogger(Uploader.class);

    public static final String ROOT_PATH = "upload";

    public static final String FILE_SEPARATOR = "/";

    private StandardMultipartHttpServletRequest request = null;

    // 文件允许格式
    // private String[] allowFiles = {".rar", ".doc", ".docx", ".zip", ".pdf", ".txt", ".swf", ".wmv", ".gif", ".png",
    // ".jpg", ".jpeg", ".bmp", "blob", ".mp4"};

    // 文件大小限制，单位KB
    private int maxSize = 10000000;

    List<UploadFile> saveUploadFileList = new ArrayList<UploadFile>();

    public Uploader(HttpServletRequest request) {
        this.request = (StandardMultipartHttpServletRequest)request;
        saveUploadFileList = new ArrayList<>();
    }

    public List<UploadFile> upload() {

        // 判断enctype属性是否为multipart/form-data
        boolean isMultipart = ServletFileUpload.isMultipartContent(this.request);
        if (!isMultipart) {
            UploadFile uploadFile = new UploadFile();
            uploadFile.setSuccess(0);
            uploadFile.setMessage("未包含文件上传域");
            saveUploadFileList.add(uploadFile);
            return saveUploadFileList;
        }
        DiskFileItemFactory dff = new DiskFileItemFactory();// 1、创建工厂
        String savePath = getSaveFilePath(ROOT_PATH);
        dff.setRepository(new File(savePath));
        try {
            ServletFileUpload sfu = new ServletFileUpload(dff);// 2、创建文件上传解析器
            sfu.setSizeMax(this.maxSize * 1024L);
            sfu.setHeaderEncoding("utf-8");// 3、解决文件名的中文乱码
            Iterator<String> iter = this.request.getFileNames();
            while (iter.hasNext()) {
                doUpload(savePath, iter);
            }
        } catch (IOException e) {
            UploadFile uploadFile = new UploadFile();
            uploadFile.setSuccess(1);
            uploadFile.setMessage("未知错误");
            saveUploadFileList.add(uploadFile);
            e.printStackTrace();
        }

        return saveUploadFileList;
    }

    private void doUpload(String savePath, Iterator<String> iter) throws IOException {
        UploadFile uploadFile = new UploadFile();
        MultipartFile multipartfile = this.request.getFile(iter.next());

        InputStream inputStream = multipartfile.getInputStream();
        String timeStampName = getTimeStampName();

        String originalName = multipartfile.getOriginalFilename();

        String fileName = getFileName(originalName);

        String fileType = FileUtils.getFileType(originalName);
        uploadFile.setFileName(fileName);
        uploadFile.setFileType(fileType);
        uploadFile.setTimeStampName(timeStampName);

        String saveFilePath = savePath + FILE_SEPARATOR + timeStampName + "." + fileType;
        String minFilePath = savePath + FILE_SEPARATOR + timeStampName + "_min" + "." + fileType;
        String ossFilePath = savePath + FILE_SEPARATOR + timeStampName + FILE_SEPARATOR + fileName + "." + fileType;

        File file = new File(PathUtils.getStaticPath() + FILE_SEPARATOR + saveFilePath);
        File minFile = new File(PathUtils.getStaticPath() + FILE_SEPARATOR + minFilePath);
        uploadFile.setIsOSS(0);
        uploadFile.setUrl(saveFilePath);
        BufferedInputStream in = null;
        FileOutputStream out = null;
        BufferedOutputStream output = null;

        try {
            in = new BufferedInputStream(inputStream);
            out = new FileOutputStream(file);
            output = new BufferedOutputStream(out);
            Streams.copy(in, output, true);
            if (FileUtils.isImageFile(uploadFile.getFileType())) {
                ImageUtils.thumbnailsImage(file, minFile, 300);
            }

        } catch (FileNotFoundException e) {
            LOG.error("文件没有发现" + e);
        } catch (IOException e) {
            LOG.error("文件读取失败" + e);
        } finally {
            closeStream(in, out, output);
        }

        uploadFile.setSuccess(1);
        uploadFile.setMessage("上传成功");
        uploadFile.setFileSize(request.getContentLengthLong());
        saveUploadFileList.add(uploadFile);
    }

    private void closeStream(BufferedInputStream in, FileOutputStream out, BufferedOutputStream output)
        throws IOException {
        if (in != null) {
            in.close();
        }
        if (out != null) {
            out.close();
        }
        if (output != null) {
            output.close();
        }
    }

    private String getFileName(String fileName) {
        return fileName.substring(0, fileName.lastIndexOf("."));
    }

    /**
     * 依据原始文件名生成新文件名
     *
     * @return
     */
    private String getTimeStampName() {
        try {
            SecureRandom number = SecureRandom.getInstance("SHA1PRNG");
            return "" + number.nextInt(10000) + System.currentTimeMillis();
        } catch (NoSuchAlgorithmException e) {
            LOG.error("生成安全随机数失败");
        }
        return "" + System.currentTimeMillis();

    }

    /**
     * 根据字符串创建本地目录 并按照日期建立子目录返回
     *
     * @param path
     * @return
     */
    private String getSaveFilePath(String path) {
        SimpleDateFormat formater = new SimpleDateFormat("yyyyMMdd");
        path = FILE_SEPARATOR + path + FILE_SEPARATOR + formater.format(new Date());
        File dir = new File(PathUtils.getStaticPath() + path);
        // LOG.error(PathUtil.getStaticPath() + path);
        if (!dir.exists()) {
            try {
                boolean isSuccessMakeDir = dir.mkdirs();
                if (!isSuccessMakeDir) {
                    LOG.error("目录创建失败:" + PathUtils.getStaticPath() + path);
                }
            } catch (Exception e) {
                LOG.error("目录创建失败" + PathUtils.getStaticPath() + path);
                return "";
            }
        }
        return path;
    }

}