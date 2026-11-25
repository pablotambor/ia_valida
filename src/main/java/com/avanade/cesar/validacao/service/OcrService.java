package com.avanade.cesar.validacao.service;

import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;

@Service
public class OcrService {

    private static final Logger log = LoggerFactory.getLogger(OcrService.class);

    @Value("${app.ocr.datapath:./tessdata}")
    private String datapath;

    @Value("${app.ocr.language:por}")
    private String language;

    public String extrairTexto(BufferedImage imagemTratada) throws TesseractException {
        ITesseract tesseract = new Tesseract();
        tesseract.setDatapath(datapath);
        tesseract.setLanguage(language);
        tesseract.setOcrEngineMode(1); // LSTM only
        tesseract.setPageSegMode(3);   // Fully automatic page segmentation

        log.info("Executando OCR com Tesseract (datapath={}, lang={})", datapath, language);
        String resultado = tesseract.doOCR(imagemTratada);
        return resultado != null ? resultado.trim() : "";
    }
}
