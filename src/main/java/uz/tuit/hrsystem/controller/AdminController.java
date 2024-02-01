package uz.tuit.hrsystem.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import uz.tuit.hrsystem.entity.Product;
import uz.tuit.hrsystem.service.UserService;

@RestController
@CrossOrigin
@RequestMapping("/api")
@SecurityRequirement(name = "bearerAuth")
public class AdminController {

    @Autowired
    UserService userService;

    @GetMapping("/getAllUsersInfo")
    @PreAuthorize("hasRole('ADMIN')")
    public HttpEntity<?> getAllUsersInfo() {
        return ResponseEntity.ok(userService.getUsersInfos());
    }

    @GetMapping("/getProductImg/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getProductImg(@PathVariable("id") Long id) {
        Resource resource = userService.getProductImg(id);

        String contentType = "application/octet-stream";
        String headerValue = "attachment; filename=\"" + resource.getFilename() + "\"";

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, headerValue)
                .body(resource);
    }

    @GetMapping("/getAllProductHistory")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public HttpEntity<?> getAllProductHistory() {
        return ResponseEntity.ok(userService.getAllProductHistory());
    }

    @PostMapping("/createProduct")
    @PreAuthorize("hasRole('ADMIN')")
    public HttpEntity<?> createProduct(@RequestBody Product product) {
        return ResponseEntity.ok(userService.addProduct(product));
    }

    @PutMapping("/editProduct")
    @PreAuthorize("hasRole('ADMIN')")
    public HttpEntity<?> editProduct(@RequestBody Product product) {
        return ResponseEntity.ok(userService.editProduct(product));
    }

    @GetMapping("/hello2")
    @PreAuthorize("hasAnyRole({'ADMIN','USER'})")
    public ResponseEntity<?> hello() {
        return ResponseEntity.ok("hello");
    }
}