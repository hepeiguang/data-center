package org.apache.seatunnel.transform.decrypt;

import lombok.Data;
import org.apache.seatunnel.shade.com.fasterxml.jackson.annotation.JsonAlias;

import java.io.Serializable;

@Data
public class DecryptMapping implements Serializable {

    @JsonAlias("decrypt_field")
    private String decryptField;

    @JsonAlias("decrypt_type")
    private String decryptType;

    @JsonAlias("secret_key")
    private String secretKey;

}