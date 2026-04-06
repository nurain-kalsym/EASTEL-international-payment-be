package com.kalsym.internationalPayment.utility;

import java.io.InputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.commons.compress.utils.IOUtils;

public class ImageUtils {
    public static byte[] fetchImageBytesFromUrl(String imageUrl) throws IOException {
        URL url = new URL(imageUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        try {
            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                InputStream inputStream = connection.getInputStream();
                return IOUtils.toByteArray(inputStream);
            } else {
                throw new IOException("Failed to fetch image from URL: " + imageUrl);
            }
        } finally {
            connection.disconnect();
        }
    }
}
