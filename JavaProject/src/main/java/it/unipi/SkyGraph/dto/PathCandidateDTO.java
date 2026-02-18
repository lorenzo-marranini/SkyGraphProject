package it.unipi.SkyGraph.dto;

import lombok.Data;
import java.util.List;

@Data
public class PathCandidateDTO {
    // Questo nome deve corrispondere all'alias nella query "AS iatas"
    private List<String> iatas;
}