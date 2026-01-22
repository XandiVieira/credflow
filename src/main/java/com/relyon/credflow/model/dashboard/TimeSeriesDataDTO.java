package com.relyon.credflow.model.dashboard;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimeSeriesDataDTO {
    private List<TimeSeriesPointDTO> dataPoints;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TimeSeriesPointDTO {
        private LocalDate date;
        private BigDecimal value;
    }
}
