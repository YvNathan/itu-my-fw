package itu.myframework.upload;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import itu.myframework.config.AppProperties;
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
    
    public String save() throws IOException {
        return  save(getUploaddir());
    }

    public String save(Path path) throws IOException {
        Path base = path.toAbsolutePath().normalize();
        Files.createDirectories(base);

        String ext = extension == null ? "" : extension.replaceAll("[^A-Za-z0-9]", "");
        String nomGenere = UUID.randomUUID() + (ext.isEmpty() ? ""  : "." + ext);

        Path cible = base.resolve(nomGenere).normalize();
        if (!cible.startsWith(base)) {
            throw new IOException("Chemin de destination refusé : " + cible);
        }

        Files.write(cible, contenu == null ? new byte[0] : contenu);
        return nomGenere;
    }

    public static Path getChemin(String nomGenere) {
        Path base = getUploaddir().toAbsolutePath().normalize();
        Path chemin = base.resolve(nomGenere).normalize();
        if (!chemin.startsWith(base)) {
            throw new IllegalArgumentException("Nom de fichier refusé : " + nomGenere);
        }
        return chemin;
    }

    private static Path getUploaddir(){
        String dir = AppProperties.get("upload.dir");
        if (dir == null || dir.isBlank()) {
            throw new IllegalStateException("upload.dir n'est pas défini dans application.properties");
        }
        return Paths.get(dir);
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
