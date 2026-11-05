package com.lhyf.dataasset;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import com.lhyf.dataasset.config.DataAssetProperties;

@SpringBootApplication
@EnableConfigurationProperties(DataAssetProperties.class)
public class DataAssetApplication {

    public static void main(String[] args) {
        SpringApplication.run(DataAssetApplication.class, args);
    }
}
