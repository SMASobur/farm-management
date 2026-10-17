package tc.impact.farmmanagement.dto.response;

import lombok.Data;

import java.util.List;

@Data
public class DashboardAlertsResponse {
    private int totalCount;
    private List<DashboardAlert> alerts;
}