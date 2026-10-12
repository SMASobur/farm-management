package tc.impact.farmmanagement.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tc.impact.farmmanagement.dto.response.ApiResponse;
import tc.impact.farmmanagement.dto.response.FarmResponse;
import tc.impact.farmmanagement.service.FarmService;

@RestController
@RequestMapping("/api/farms")
@RequiredArgsConstructor
public class FarmController {

    private final FarmService farmService;

    @GetMapping("/current")
    public ResponseEntity<ApiResponse<FarmResponse>> getCurrent() {
        return ResponseEntity.ok(ApiResponse.success(farmService.getCurrentFarm()));
    }

    @PostMapping(value = "/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<FarmResponse>> uploadLogo(
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(ApiResponse.success("Logo uploaded", farmService.uploadLogo(file)));
    }

    @DeleteMapping("/logo")
    public ResponseEntity<ApiResponse<FarmResponse>> removeLogo() {
        return ResponseEntity.ok(ApiResponse.success("Logo removed", farmService.removeLogo()));
    }
}