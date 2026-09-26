package com.hnkjzyxy.ab.utils;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 这是一个pdf变成word的工具，也就是读取pdf文件，并且把pdf文件变成图片，插入到word中
 */
public class PdfToWordConverter {


    /**
     * 测试代码
     *
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
//        File filepath = new File("D:\\pdf2");

//        String path="D:\\uploadFile\\202331";
//        String wordFilePath = "D:\\2.docx";
//        //需要传入的参数，pdf存在的根目录（即员工的工号）和指定的word的目录
//        pdfFilesToWordFile(path,wordFilePath);
//        System.out.println("文件生成");
//        String pathSeparator = FilePathUtils.getRealFilePath("/");
//        System.out.println(pathSeparator);
    }


    /**
     * 将多个pdf文件插入到一个word文档
     *
     * @param path         pdf文件的文件存在的根路径
     * @param wordFilePath 指定的word文件路径
     * @throws Exception
     */
    public static void pdfFilesToWordFile(String path, String wordFilePath) throws Exception {
        //1、 读取pdf文件
        File filepath = new File(path);
        String[] pdfFileList = filepath.list();

        //2、 创建一个新的Word文档
        XWPFDocument doc = new XWPFDocument();
        // 创建一个段落
        XWPFParagraph p = doc.createParagraph();

        // 在流被关闭之后删除图片目录
        List<String> list = new ArrayList<>();

        String pathSeparator = FilePathUtils.getRealFilePath("/");
        // 3、遍历文件路径清单
        for (String filePath : pdfFileList) {
            // 将PDF转换为图片，该方法返回图片的路径 TODO 文件的路径问题
            String imagePath = convertPdfToImage(path + pathSeparator + filePath);
            // 在段落中插入图片
            XWPFRun r = p.createRun();
            //TODO 调整比率
            FileInputStream fileInputStream = new FileInputStream(imagePath);
            // 图片的宽度为400像素，高度为600像素
            r.addPicture(fileInputStream,
                    XWPFDocument.PICTURE_TYPE_PNG,
                    imagePath,
                    Units.toEMU(400),
                    Units.toEMU(600));

            fileInputStream.close();

            // 记录图片路径，用于删除
            list.add(imagePath);
        }
        // 4、保存文档
        FileOutputStream out = new FileOutputStream(wordFilePath);
        doc.write(out);
        out.close();
        doc.close();

        //5、删除图片文件
        for (String imagePath : list) {
            File imageFile = new File(imagePath);
            if (imageFile.exists()) {
                boolean delete = imageFile.delete();
                if (delete) {
                    System.out.println("删除图片成功");
                } else {
                    System.out.println("删除图片失败");
                }
            } else {
                System.out.println("图片不存在");
            }
        }
    }

    /**
     * pdf转图片png
     *
     * @param filePath 文件路径名
     * @return 图片路径名
     * @throws IOException
     */
    private static String convertPdfToImage(String filePath) throws IOException {


        PDDocument document = null;
        // 读取PDF文件
        File pdfFile = new File(filePath);

        //同目录下生成同一文件名的图片
        String pngFilePath = pdfFile.getParent() + File.separator + getFileNameWithoutExtension(pdfFile) + ".png";

        File file = new File(pngFilePath);
        if (!file.exists()) {
            document = PDDocument.load(pdfFile);
            PDFRenderer renderer = new PDFRenderer(document);
            // TODO 目前只考虑渲染PDF文件第一页为图像，如果PDF有多页，则需要考虑遍历
            BufferedImage image = renderer.renderImage(0);
            ImageIO.write(image, "PNG", new File(pngFilePath));
        }
        //关闭文档
        if (document != null) {
            document.close();
        }

        //返回图片路径
        return pngFilePath;
    }

    /**
     * 去掉文件扩展名
     *
     * @param file
     * @return
     */
    private static String getFileNameWithoutExtension(File file) {
        String name = file.getName();
        int lastIndexOf = name.lastIndexOf('.');
        if (lastIndexOf > 0) {
            // 如果存在扩展名，则去掉它
            return name.substring(0, lastIndexOf);
        } else {
            // 否则，文件名不包含扩展名
            return name;
        }
    }
}
