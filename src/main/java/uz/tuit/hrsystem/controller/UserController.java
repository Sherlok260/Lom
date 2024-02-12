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
        return ResponseEntity.ok(userService.signUp(dto));
    }

    @PostMapping("/registerConfirm")
    @PreAuthorize("hasRole('AUTH')")
    public ResponseEntity<?> registerConfirm(@RequestParam String code) {
        return ResponseEntity.ok(userService.registerConfirm(code));
    }

    @GetMapping("/forgetPassword")
    public HttpEntity<?> forgetPassword(@RequestParam String email) {
        return ResponseEntity.ok(userService.forgetPassword(email));
    }

    @GetMapping("/forgetPasswordConfirm")
    @PreAuthorize("hasRole('AUTH')")
    public HttpEntity<?> forgetPasswordConfirm(@RequestParam String code) {
        return ResponseEntity.ok(userService.forgetPasswordConfirm(code));
    }

    @GetMapping("/setNewPassword")
    @PreAuthorize("hasRole('AUTH')")
    public HttpEntity<?> setNewPassword(@RequestParam String confirmCode,@RequestParam String newPassword) {
        return ResponseEntity.ok(userService.setNewPassword(confirmCode, newPassword));
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
            @RequestParam("branch") String branch,
            @RequestParam("department") String department,
            @NonNull @RequestParam("file") MultipartFile multipartFile)
    {
        ApiResponse apiResponse = userService.addProduct(product_name, weight, branch, department, multipartFile);
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

    @GetMapping("/getProductHistory")
    @PreAuthorize("hasAnyRole({'USER', 'ADMIN'})")
    public ResponseEntity<?> getProductHistory() {
        return ResponseEntity.ok(userService.getProductHistory());
    }

//    @GetMapping("/getAllBranch")
//    @PreAuthorize("hasAnyRole({'USER', 'ADMIN'})")
//    public ResponseEntity<?> getAllBranch() {
//        return ResponseEntity.ok(userService.getAllBranch());
//    }
//
//    @PostMapping("/createBranch")
//    @PreAuthorize("hasAnyRole({'USER', 'ADMIN'})")
//    public ResponseEntity<?> createBranch(@RequestParam String branchName) {
//        return ResponseEntity.ok(userService.createBranch(branchName));
//    }
//
//    @DeleteMapping("/deleteBranch")
//    @PreAuthorize("hasAnyRole({'USER', 'ADMIN'})")
//    public ResponseEntity<?> deleteBranch(@RequestParam String branchName) {
//        return ResponseEntity.ok(userService.deleteBranch(branchName));
//    }
//
//    @PostMapping("/addDepartment")
//    @PreAuthorize("hasAnyRole({'USER', 'ADMIN'})")
//    public ResponseEntity<?> addDepartment(@RequestParam String branchName ,@RequestParam String departmentName) {
//        return ResponseEntity.ok(userService.addDepartment(branchName, departmentName));
//    }
//
//    @DeleteMapping("/deleteDepartment")
//    @PreAuthorize("hasAnyRole({'USER', 'ADMIN'})")
//    public ResponseEntity<?> deleteDepartment(@RequestParam String branchName, @RequestParam String departmentName) {
//        return ResponseEntity.ok(userService.deleteDepartment(branchName, departmentName));
//    }
}