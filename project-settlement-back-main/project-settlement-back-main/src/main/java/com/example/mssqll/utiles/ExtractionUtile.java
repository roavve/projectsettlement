package com.example.mssqll.utiles;

import com.example.mssqll.models.Extraction;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.stream.Stream;

import static com.example.mssqll.utiles.SecurityUtils.getCurrentUsername;

@Slf4j
public class ExtractionUtile {
    public static Long sum(List<Extraction> extList){
        log.debug("Calculating sum for extraction list with {} items (by {})", extList != null ? extList.size() : 0, getCurrentUsername());
        Long sum = 0L;
        Stream.of(extList).forEach(ext->{
        });
        log.debug("Sum calculation completed: {} (by {})", sum, getCurrentUsername());
        return sum;
    }

}
