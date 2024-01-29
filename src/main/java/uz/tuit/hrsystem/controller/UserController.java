package uz.tuit.hrsystem.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import uz.tuit.hrsystem.payload.ApiResponse;
import uz.tuit.hrsystem.payload.LoginDto;
import uz.tuit.hrsystem.payload.RegisterDto;
import uz.tuit.hrsystem.repository.ProductRepository;
import uz.tuit.hrsystem.service.UserService;

@RestController
@RequestMapping("/api")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    @Autowired
    UserService userService;

    @Autowired
    ProductRepository productRepository;

    @PostMapping("/login")
    public HttpEntity<?> login(@RequestBody LoginDto dto) {
        return ResponseEntity.ok().body(userService.login(dto));
    }

    @PostMapping("/signUp")
    public ResponseEntity<?> register(@RequestBody RegisterDto dto) {
        return ResponseEntity.ok(userService.signUp2(dto));
    }

    @GetMapping("/refreshToken")
    public ResponseEntity<?> refreshToken(@RequestParam("token") String token) {
        return ResponseEntity.ok(userService.refreshToken(token));
    }

    @GetMapping("/logout")
    @PreAuthorize("hasAnyRole({'ADMIN','USER'})")
    public ResponseEntity<?> logout() {
        return ResponseEntity.ok(userService.logout());
    }

    @PostMapping(value = "/addProduct", consumes = {"multipart/form-data;application/json"})
    @PreAuthorize("hasAnyRole({'ADMIN','USER'})")
    public HttpEntity<?> addProduct(
            @RequestParam("product") String product_name,
            @RequestParam("weight") double weight,
            @NonNull @RequestParam("file") MultipartFile multipartFile)
    {
        ApiResponse apiResponse = userService.addProduct(product_name, weight, multipartFile);
        return ResponseEntity.ok(apiResponse);
    }

    @GetMapping("/getUserInfo")
    @PreAuthorize("hasAnyRole({'USER', 'ADMIN'})")
    public ResponseEntity<?> getUserInfo() {
        return ResponseEntity.ok(userService.getUserInfo());
    }

    @GetMapping("/getAllProduct")
    @PreAuthorize("hasAnyRole({'USER', 'ADMIN'})")
    public ResponseEntity<?> getAllProducts() {
        return ResponseEntity.ok(userService.getAllProductList());
    }
}