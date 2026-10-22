package org.apache.seatunnel.transform.switchcase;

import lombok.Data;
import org.apache.seatunnel.shade.com.fasterxml.jackson.annotation.JsonAlias;

import java.io.Serializable;

@Data
public class SwitchCaseMapping implements Serializable {
    @JsonAlias("switch_case_when")
    private Object switchCaseWhen;

    @JsonAlias("switch_case_then")
    private Object switchCaseThen;

}