package itu.myframework.upload;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;

import jakarta.servlet.http.Part;

public class FileUpload {
    private String nom;
    private String extension;
    private String contentType;
    private byte[] contenu;

    public FileUpload() {
    }

    public static FileUpload fromPart(Part part) throws IOException {
        FileUpload fileUpload = new FileUpload();

        String fullNom = Paths.get(part.getSubmittedFileName()).getFileName().toString();

        int placePoint = fullNom.lastIndexOf('.');
        if (placePoint > 0) {
            fileUpload.nom = fullNom.substring(0, placePoint);
            fileUpload.extension = fullNom.substring(placePoint + 1);
        } else {
            fileUpload.nom = fullNom;
            fileUpload.extension = "";
        }

        fileUpload.contentType = part.getContentType();

        try (InputStream in = part.getInputStream()) {
            fileUpload.contenu = in.readAllBytes();
        }

        return fileUpload;
    }

    public String getNomComplet() {
        return extension.isEmpty() ? nom : nom + "." + extension;
    }

    public long getTaille() {
        return contenu == null ? 0 : contenu.length;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getExtension() {
        return extension;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public byte[] getContenu() {
        return contenu;
    }

    public void setContenu(byte[] contenu) {
        this.contenu = contenu;
    }
}
