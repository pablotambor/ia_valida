package com.avanade.cesar.validacao.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Size;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

@Service
public class ImagePreprocessingService {

    private static final Logger log = LoggerFactory.getLogger(ImagePreprocessingService.class);

    /**
     * Converte o upload (PDF ou Imagem) em BufferedImage e aplica o pipeline OpenCV:
     * 1. Escala de cinza
     * 2. Remoção de ruído (GaussianBlur)
     * 3. Ajuste de contraste / binarização adaptativa (Otsu)
     */
    public BufferedImage carregarETratar(MultipartFile file) throws IOException {
        BufferedImage original = converterParaBufferedImage(file);
        if (original == null) {
            throw new IOException("Não foi possível ler a imagem ou PDF enviado.");
        }

        try {
            return aplicarFiltrosOpenCV(original);
        } catch (UnsatisfiedLinkError | NoClassDefFoundError | Exception ex) {
            log.warn("OpenCV indisponível neste ambiente ({}). Prosseguindo com imagem original.", ex.getMessage());
            return original;
        }
    }

    private BufferedImage converterParaBufferedImage(MultipartFile file) throws IOException {
        String filename = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";
        String contentType = file.getContentType() != null ? file.getContentType().toLowerCase() : "";

        if (filename.endsWith(".pdf") || contentType.contains("pdf")) {
            try (PDDocument document = Loader.loadPDF(file.getBytes())) {
                PDFRenderer renderer = new PDFRenderer(document);
                // Renderiza a primeira página do comprovante em 300 DPI para boa leitura de OCR
                return renderer.renderImageWithDPI(0, 300, ImageType.RGB);
            }
        }

        try (InputStream is = file.getInputStream()) {
            return ImageIO.read(is);
        }
    }

    private BufferedImage aplicarFiltrosOpenCV(BufferedImage bufferedImage) {
        Java2DFrameConverter java2DConverter = new Java2DFrameConverter();
        OpenCVFrameConverter.ToMat matConverter = new OpenCVFrameConverter.ToMat();

        Mat source = matConverter.convert(java2DConverter.convert(bufferedImage));
        Mat gray = new Mat();
        Mat blurred = new Mat();
        Mat thresholded = new Mat();

        try {
            // 1. Escala de cinza
            if (source.channels() >= 3) {
                opencv_imgproc.cvtColor(source, gray, opencv_imgproc.COLOR_BGR2GRAY);
            } else {
                source.copyTo(gray);
            }

            // 2. Suavização / remoção de ruído
            opencv_imgproc.GaussianBlur(gray, blurred, new Size(3, 3), 0);

            // 3. Binarização adaptativa (melhora contraste para o Tesseract)
            opencv_imgproc.threshold(
                    blurred,
                    thresholded,
                    0,
                    255,
                    opencv_imgproc.THRESH_BINARY | opencv_imgproc.THRESH_OTSU
            );

            return java2DConverter.convert(matConverter.convert(thresholded));
        } finally {
            source.release();
            gray.release();
            blurred.release();
            thresholded.release();
        }
    }
}
