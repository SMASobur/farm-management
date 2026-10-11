package tc.impact.farmmanagement.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tc.impact.farmmanagement.dto.request.AnimalRequest;
import tc.impact.farmmanagement.dto.response.AnimalResponse;
import tc.impact.farmmanagement.dto.response.ApiResponse;
import tc.impact.farmmanagement.entity.Animal;
import tc.impact.farmmanagement.service.AnimalService;

import java.util.List;

@RestController
@RequestMapping("/api/animals")
@RequiredArgsConstructor
public class AnimalController {

    private final AnimalService animalService;

    @PostMapping
    public ResponseEntity<ApiResponse<AnimalResponse>> createAnimal(@Valid @RequestBody AnimalRequest request) {
        AnimalResponse animal = animalService.createAnimal(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Animal created successfully", animal));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<AnimalResponse>>> getAllAnimals(
            @RequestParam(required = false) Long speciesId,
            @RequestParam(required = false) Animal.AnimalStatus status) {

        List<AnimalResponse> animals;
        if (speciesId != null && status != null) {
            animals = animalService.getAnimalsBySpeciesAndStatus(speciesId, status);
        } else if (speciesId != null) {
            animals = animalService.getAnimalsBySpecies(speciesId);
        } else if (status != null) {
            animals = animalService.getAnimalsByStatus(status);
        } else {
            animals = animalService.getAllAnimals();
        }
        return ResponseEntity.ok(ApiResponse.success(animals));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AnimalResponse>> getAnimalById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(animalService.getAnimalById(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<AnimalResponse>> updateAnimal(
            @PathVariable Long id,
            @Valid @RequestBody AnimalRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Animal updated", animalService.updateAnimal(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteAnimal(@PathVariable Long id) {
        animalService.deleteAnimal(id);
        return ResponseEntity.ok(ApiResponse.success("Animal deleted", null));
    }
}