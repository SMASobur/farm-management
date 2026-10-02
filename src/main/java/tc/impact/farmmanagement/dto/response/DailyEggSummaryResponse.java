package tc.impact.farmmanagement.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DailyEggSummaryResponse {
    private LocalDate date;
    private Long totalEggs;
}