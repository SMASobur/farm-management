package tc.impact.farmmanagement.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import tc.impact.farmmanagement.dto.response.FarmResponse;
import tc.impact.farmmanagement.entity.Farm;
import tc.impact.farmmanagement.exception.BusinessException;
import tc.impact.farmmanagement.repository.FarmRepository;

@Service
@RequiredArgsConstructor
public class FarmService {

    private final FarmRepository farmRepository;
    private final CloudinaryService cloudinaryService;
    private final FarmContext farmContext;

    @Transactional(readOnly = true)
    public FarmResponse getCurrentFarm() {
        return toResponse(farmContext.getCurrentFarm());
    }

    @Transactional
    public FarmResponse uploadLogo(MultipartFile file) {
        Farm farm = farmContext.getCurrentFarm();
        String folder = "farm-management/logos/" + farm.getId();
        String logoUrl = cloudinaryService.uploadImage(file, folder);
        farm.setLogoUrl(logoUrl);
        return toResponse(farmRepository.save(farm));
    }

    @Transactional
    public FarmResponse removeLogo() {
        Farm farm = farmContext.getCurrentFarm();
        farm.setLogoUrl(null);
        // Optional: also delete from Cloudinary by public_id — requires storing public_id
        return toResponse(farmRepository.save(farm));
    }

    private FarmResponse toResponse(Farm f) {
        FarmResponse r = new FarmResponse();
        r.setId(f.getId());
        r.setCode(f.getCode());
        r.setName(f.getName());
        r.setLocation(f.getLocation());
        r.setOwnerName(f.getOwnerName());
        r.setOwnerPhone(f.getOwnerPhone());
        r.setOwnerEmail(f.getOwnerEmail());
        r.setLogoUrl(f.getLogoUrl());
        r.setPlan(f.getPlan());
        r.setActive(f.isActive());
        r.setCreatedAt(f.getCreatedAt());
        return r;
    }
}