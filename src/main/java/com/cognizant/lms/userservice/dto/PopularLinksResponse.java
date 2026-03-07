package com.cognizant.lms.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PopularLinksResponse {
    private List<PopularLinkDto> popularLinkList;
    private int count;
    // Optionally, add more metadata fields here (e.g., totalCount, page, perPage)
}
