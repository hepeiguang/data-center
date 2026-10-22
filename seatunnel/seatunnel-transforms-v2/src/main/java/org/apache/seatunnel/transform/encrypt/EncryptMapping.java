package org.apache.seatunnel.transform.encrypt;

import lombok.Data;
import org.apache.seatunnel.shade.com.fasterxml.jackson.annotation.JsonAlias;

import java.io.Serializable;

@Data
public class EncryptMapping implements Serializable {

    @JsonAlias("encrypt_field")
    private String encryptField;

    @JsonAlias("encrypt_type")
    private String encryptType;

    @JsonAlias("secret_key")
    private String secretKey;

}