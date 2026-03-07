package com.cognizant.lms.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class LookupDto {

    private String pk;
    private String sk;
    private String name;

    public LookupDto(String name){
        this.name = name;
    }
}
