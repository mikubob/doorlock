package com.hnkjzyxy.ab.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;

/**
 * 上传文件存储、下载响应及压缩包处理工具
 */
@Component
@Slf4j
public class UploadUtils {

    /**
     * 图片文件存储根目录
     */
    @Value("${upload.imgUrl}")
    private String imgUrl;
    /**
     * 佐证材料文件存储根目录
     */
    @Value("${upload.fileUrl}")
    private String fileUrl;
    /**
     * 建设项目材料文件存储根目录
     */
    @Value("${upload.conFileUrl}")
    private String conFileUrl;

    /**
     * 雪花ID生成工具
     */
    @Resource
    private SnowFlowUtils snowFlowUtils;

    /**
     * 将指定本地文件写入下载响应
     *
     * @param response HTTP 响应对象
     * @param downFileUrl 待下载文件的本地路径
     * @throws IOException 文件读取或输出失败时抛出
     */
    public static void download(HttpServletResponse response, String downFileUrl) throws IOException {
        response.setCharacterEncoding("utf8");
        //定义文件路径
        File file = new File(downFileUrl);
        InputStream is = null;
        OutputStream os = null;
        try {
            //分片下载
            long fSize = file.length();//获取长度
            response.setContentType("application/x-download");
            String fileName = URLEncoder.encode(file.getName(), "utf8");
            response.addHeader("Content-Disposition", "attachment;filename=" + fileName);
            response.setHeader("Accept-Range", "bytes");
            response.addHeader("Access-Contro1-Allow-Origin", "*");
            //获取文件大小
            response.setHeader("fSize", String.valueOf(fSize));
            response.setHeader("fName", fileName);
            //定义断点
            long pos = 0, last = fSize - 1, sum = 0;
            long rangeLenght = last - pos + 1;
            String contentRange = new StringBuffer("bytes").append(pos).append("-").append(last).append("/").append(fSize).toString();
            response.setHeader("Content-Range", contentRange);
            response.setHeader("Content-Lenght", String.valueOf(rangeLenght));
            os = new BufferedOutputStream(response.getOutputStream());
            is = new BufferedInputStream(new FileInputStream(file));
            is.skip(pos);//跳过已读的文件
            byte[] buffer = new byte[1024];
            int lenght = 0;
            //相等证明读完
            while (sum < rangeLenght) {
                lenght = is.read(buffer, 0, (rangeLenght - sum) <= buffer.length ? (int) (rangeLenght - sum) : buffer.length);
                sum = sum + lenght;
                os.write(buffer, 0, lenght);
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (is != null) {
                is.close();
            }
            if (os != null) {
                os.close();
            }
        }
    }

    /**
     * 上传头像或电子签名图片
     *
     * @param file 待处理文件
     * @param avatar 原头像或电子签名访问路径
     * @param userName 用户名（工号）
     * @param flag 处理标记
     * @return 上传成功时返回文件访问路径，失败或文件为空时返回空 Optional
     */
    public Optional<String> uploadImg(MultipartFile file, String avatar, String userName, String flag) {

        //1 判断是否为空
        if (file.isEmpty()) {
            return Optional.empty();
        }

        //2 拿到类型
        String s = file.getOriginalFilename();
        String[] split = s.split("\\.");
        String newFileName = "";


        //3 进行处理
        try {
            String substring = avatar.substring(avatar.lastIndexOf(File.separator) + 1);
            File delFile = new File(imgUrl + substring);
            if (delFile.exists()) {
                delFile.delete();
            }
            //用户名+分隔符
            String dir = userName.concat(File.separator);

            // 判断是否为电子签名 判断是否合法 生成文件名
            if ("1".equals(flag)) {
                //dir = userName.concat("/sign/");
                dir = userName.concat(FilePathUtils.getRealFilePath("/sign/"));
                File exit = new File(imgUrl.concat(dir));
                if (exit.exists()) {
                    if (exit.list().length >= 5) {
                        throw new RuntimeException("你的电子签名个数已有五个！不能再多了！");
                    }
                }
                newFileName = snowFlowUtils.nextId() + ".png";
            } else {
                if (!s.toLowerCase().endsWith("jpg") && !s.toLowerCase().endsWith("png") && !s.toLowerCase().endsWith("jpeg")) {
                    throw new RuntimeException("只能上传jpg png jpeg格式的文件!");
                }
                newFileName = snowFlowUtils.nextId() + "." + split[split.length - 1];
            }


            File dirName = new File(imgUrl.concat(dir));
            if (!dirName.exists()) {
                dirName.mkdirs();
            }
            Files.copy(file.getInputStream(), Paths.get(imgUrl + dir).resolve(newFileName));
//                return Optional.of("/pic/file/".concat(dir).concat(newFileName));
            return Optional.of(FilePathUtils.getRealFilePath("/pic/file/").concat(dir).concat(newFileName));
        } catch (IOException e) {
            e.printStackTrace();
        }


        return Optional.empty();
    }

    /**
     * 上传用户佐证材料
     *
     * @param file 待处理文件
     * @param fileName 文件名称
     * @return 上传成功时返回文件访问路径，失败或文件为空时返回空 Optional
     */
    public Optional<String> uploadFile(MultipartFile file, String fileName) {
        if (!file.isEmpty()) {
            String filename = file.getOriginalFilename();
            String[] split = filename.split("\\.");
            String dir = fileName.concat(File.separator);
            //拿到用户文件目录
            File dirName = new File(fileUrl + dir);
            //判断用户之前是否有属于自己的目录，没有则创建，否则判断重名个数
            if (!dirName.exists()) {
                dirName.mkdirs();
            }
            String newFileName = snowFlowUtils.nextId() + ".pdf";
            try {
                Files.copy(file.getInputStream(), Paths.get(fileUrl.concat(dir)).resolve(newFileName));
//                return Optional.of("/pdf/file/".concat(dir).concat(newFileName));
                return Optional.of(FilePathUtils.getRealFilePath("/pdf/file/").concat(dir).concat(newFileName));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return Optional.empty();
    }

    /**
     * 查询指定目录下的电子签名图片访问路径
     *
     * @param dir 图片相对目录
     * @return 查询结果列表
     */
    public List<String> getSignImg(String dir) {
        File file = new File(imgUrl + dir);
        List<String> list = new ArrayList<>();
        if (file.exists() && file.isDirectory()) {
//            list = Arrays.asList(file.list()).stream().map(item -> "/pic/file/".concat(dir).concat(item) ).collect(Collectors.toList());
            list = Arrays.asList(file.list()).stream().map(item -> FilePathUtils.getRealFilePath("/pic/file/").concat(dir).concat(item)).collect(Collectors.toList());
        }
        return list;
    }

    /**
     * 按访问路径删除电子签名图片
     *
     * @param url 文件访问路径
     */
    public void delSignImg(String url) {
//        url = url.substring("/pic/file".length());
        url = url.substring(FilePathUtils.getRealFilePath("/pic/file/").length());
        File file = new File(imgUrl + url);
        // 判断文件是否存在
        if (file.exists()) {
            // 判断是否为文件
            if (file.isFile()) {  //为文件时调用删除文件方法
                file.delete();
            }
        }
    }

    /**
     * 上传建设项目材料
     *
     * @param file 待处理文件
     * @param conUrl 建设材料相对目录
     * @return 上传成功时返回文件访问路径，失败或文件为空时返回空 Optional
     */
    public Optional<String> uploadConFile(MultipartFile file, String conUrl) {
        if (!file.isEmpty()) {
            String filename = file.getOriginalFilename();
            String[] split = filename.split("\\.");
            String newFileName = snowFlowUtils.nextId() + "." + split[split.length - 1];
            //拿到文件目录
            File dirName = new File(conFileUrl + conUrl);
            //判断之前是否有属于自己的目录，没有则创建
            if (!dirName.exists()) {
                dirName.mkdirs();
            }
            try {
                Files.copy(file.getInputStream(), Paths.get(conFileUrl.concat(conUrl)).resolve(newFileName));
//                return Optional.of("/pdf/conFile/"+(conUrl + File.separator).concat(newFileName));
                return Optional.of(FilePathUtils.getRealFilePath("/pdf/conFile/") + (conUrl + File.separator).concat(newFileName));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return Optional.empty();
    }

    /**
     * 将建设项目目录中的材料打包下载
     *
     * @param conUrl 建设材料相对目录
     * @param response HTTP 响应对象
     */
    public void downloadPDFs(String conUrl, HttpServletResponse response) {
        try {
            String url = conFileUrl + conUrl;
            File file = new File(url);
            ArrayList<String> list = new ArrayList<>();
            getFiles(list, file);
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM HH:mm:ss");
            String s = format.format(new Date()).replace("-", "").replace(" ", "").replaceAll(":", "");
            File zip = compressedFileToZip(s, list);
            download(response, zip.getPath());
            if (zip.exists()) {
                zip.delete();
            }
        } catch (Exception e) {
            throw new RuntimeException("下载失败！");
        }
    }

    /**
     * 递归收集目录中的文件路径
     *
     * @param paths 接收文件路径的列表
     * @param file 待处理文件
     */
    public void getFiles(List<String> paths, File file) {
        if (file.exists() && file.isDirectory()) {
            File[] files = file.listFiles();
            Arrays.stream(files).forEach(item -> {
                if (item.isDirectory()) {
                    getFiles(paths, item);
                } else {
                    paths.add(item.getPath());
                }
            });
        }
    }

    /**
     * 将指定文件集合压缩到建设材料目录
     *
     * @param fileName 文件名称
     * @param filePathList 待处理本地文件路径列表
     * @return 生成的本地文件
     */
    public File compressedFileToZip(String fileName, List<String> filePathList) {
        String fileZip = conFileUrl + fileName + ".zip";
        OutputStream os = null;
        ZipOutputStream zos = null;
        File file = new File(fileZip);
        try {
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            os = Files.newOutputStream(file.toPath());
            zos = new ZipOutputStream(os);
            byte[] bytes = new byte[1024];
            for (String path : filePathList) {
                File pdf = new File(path);
                zos.putNextEntry(new ZipEntry(pdf.getName()));
                int len;
                FileInputStream inputStream = new FileInputStream(pdf);
                while ((len = inputStream.read(bytes)) != -1) {
                    zos.write(bytes, 0, len);
                }
                zos.closeEntry();
                inputStream.close();
            }
        } catch (Exception e) {
            throw new RuntimeException("文件下载错误！");
        } finally {
            //关闭流
            if (zos != null) {
                try {
                    zos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            //关闭流
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return file;
    }

    /**
     * 按建设材料访问路径删除对应的本地文件
     *
     * @param path 本地文件路径
     */
    public void isConFile(String path) {
//        path = path.substring("/pdf/conFile/".length());
        path = path.substring(FilePathUtils.getRealFilePath("/pdf/conFile/").length());
        File file = new File(conFileUrl + path);
        if (file.exists()) {
            file.delete();
        }
    }

    /**
     * 将佐证材料集合打包下载
     *
     * @param evidence 佐证材料路径列表
     * @param response HTTP 响应对象
     */
    public void downloadEvidence(List<String> evidence, HttpServletResponse response) {
        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM HH:mm:ss");
            String s = format.format(new Date()).replace("-", "").replace(" ", "").replaceAll(":", "");

            //通过证据集合和格式化的时间作为名字
            //创建这个需要的zip文件
            File zip = compressedAssetsToZip(s, evidence);

            //下载获得的zip文件
            //下载的方法是传入一个流，然后获得文件对象
            //然后把文件对象写到response的输出流中
            download(response, zip.getPath());

            //删除zip文件
            if (zip.exists()) {
                zip.delete();
            }
        } catch (Exception e) {
            log.info(e.getMessage());
            throw new RuntimeException("下载失败！");
        }
    }


    /**
     * 下载成zip文件
     *
     * @param fileName     zip文件的名称 : 时间戳点zip文件
     * @param filePathList 待处理本地文件路径列表
     * @return 生成的本地文件
     */
    public File compressedAssetsToZip(String fileName, List<String> filePathList) {
        //文件的名称为 时间戳点zip文件
        String fileZip = fileUrl + fileName + ".zip";
        OutputStream os = null;
        ZipOutputStream zos = null;

        //对该路径下的文件路径的抽象描述
        File file = new File(fileZip);
        try {
            if (!file.getParentFile().exists()) {
                //这里创建的父目录的名称就是uploadFile这个文件夹
                //用来确保在该路径下创建zip文件
                file.getParentFile().mkdirs();
            }

            //创建一个输出流，用来写zip文件
            //这里的输出流是用来写zip文件，所以这里的类型是ZipOutputStream
            os = Files.newOutputStream(file.toPath());
            zos = new ZipOutputStream(os);


            byte[] bytes = new byte[1024];
            for (String path : filePathList) {
                String filePath = FilePathUtils.getRealFilePath(path);
                //对每个文件进行压缩
                //这里的每个文件是指evidence集合中的每个路径
                File pdf = new File(fileUrl + filePath);


                zos.putNextEntry(new ZipEntry(pdf.getName()));
                int len;
                FileInputStream inputStream = new FileInputStream(pdf);
                while ((len = inputStream.read(bytes)) != -1) {
                    //把文件写入到zip文件中
                    zos.write(bytes, 0, len);
                }
                zos.closeEntry();
                inputStream.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("文件下载错误！");
        } finally {
            //关闭流
            if (zos != null) {
                try {
                    zos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            //关闭流
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return file;
    }


}