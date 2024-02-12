package uz.tuit.hrsystem.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import uz.tuit.hrsystem.entity.*;
import uz.tuit.hrsystem.entity.Verify;
import uz.tuit.hrsystem.jwt.JwtFilter;
import uz.tuit.hrsystem.jwt.JwtProvider;
import uz.tuit.hrsystem.payload.*;
import uz.tuit.hrsystem.repository.*;

import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.*;

import org.springframework.core.io.Resource;

@Service
public class UserService {

    @Value("${file.path}")
    String path;

    @Value("${domain.file.path}")
    String d_path;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Value("classpath:images/")
    private Resource resource;

    @Autowired
    JwtProvider jwtProvider;

    @Autowired
    TokenRepository tokenRepository;

    @Autowired
    MailService mailService;

    @Autowired
    UserRepository userRepository;

    @Autowired
    ProductRepository productRepository;

    @Autowired
    VerifyRepository verifyRepository;

    @Autowired
    RoleRepository roleRepository;

    @Autowired
    ProductHistoryRepository productHistoryRepository;

    @Autowired
    BranchRepository branchRepository;

    @Autowired
    DepartmentRepository departmentRepository;

    @Autowired
    AuthenticationManagerBuilder authenticationManagerBuilder;

    @Transactional
    public ApiResponse login(LoginDto dto) {
        try {

            Authentication authentication = authenticationManagerBuilder.getObject().authenticate(new UsernamePasswordAuthenticationToken(
                    dto.getEmail(),
                    dto.getPassword()
            ));
            SecurityContextHolder.getContext().setAuthentication(authentication);

            User user = userRepository.findByEmail(dto.getEmail()).get();

            tokenRepository.deleteByUserId(user.getId());

            String a_token = jwtProvider.generateToken(authentication, "ACCESS_TOKEN");
            String r_token = jwtProvider.generateToken(authentication, "REFRESH_TOKEN");

            Token access_token = new Token();
            access_token.setType("ACCESS_TOKEN");
            access_token.setLevel("VALID");
            access_token.setForr("ANY");
            access_token.setToken(a_token);
            access_token.setUser(user);

            Token refresh_token = new Token();
            refresh_token.setType("REFRESH_TOKEN");
            refresh_token.setLevel("VALID");
            access_token.setForr("ANY");
            refresh_token.setToken(r_token);
            refresh_token.setUser(user);

            user.getTokens().add(access_token);
            user.getTokens().add(refresh_token);

            userRepository.save(user);

            return new ApiResponse("success yaxshi", true, new AccessRefreshTokenDto(a_token, r_token));
        } catch (Exception e) {
            e.printStackTrace();
            return new ApiResponse("email or password incorrect", false);
        }
    }

    @Transactional
    public ApiResponse logout() {
        try {
            String email = JwtFilter.getEmail;
            User user = userRepository.findByEmail(email).get();
//            tokenRepository.deleteAll(user.getTokens());
            tokenRepository.deleteByUserId(user.getId());
            return new ApiResponse("success logout", true);
        } catch (Exception e) {
            e.printStackTrace();
            return new ApiResponse(e.getMessage(), false);
        }
    }

    @Transactional
    public ApiResponse forgetPassword(String email) {
        try {
            Optional<User> user = userRepository.findByEmail(email);
            if (user.isPresent() && user.get().isVerified() && user.get().getRoles().stream().anyMatch(r -> r.getName().equals("USER"))) {
                Optional<Token> optionalToken = user.get().getTokens().stream().filter(t -> t.getLevel().equals("TEMPORARY") && t.getForr().equals("FORGET_PASSWORD")).findFirst();
                String token = jwtProvider.generateTokenForVerify(user.get().getUsername());
                if (optionalToken.isPresent()) {
                    if (jwtProvider.validateToken(optionalToken.get().getToken())) {
                        return new ApiResponse("Parolni qayta tiklash uchun kod emailingizga yuborilgan. Iltimos Emailingizni tekshiring", true);
                    } else {
                        optionalToken.get().setToken(token);
                        tokenRepository.save(optionalToken.get());
                    }
                } else {
                    System.out.println("-----------------------------------------------------------------------------");
                    System.out.println(token);
                    System.out.println("------------------------------------------------------------------------");
                    Token temporary_token = new Token();
                    temporary_token.setUser(user.get());
                    temporary_token.setToken(token);
                    temporary_token.setLevel("TEMPORARY");
                    temporary_token.setType("ACCESS_TOKEN");
                    temporary_token.setForr("FORGET_PASSWORD");
                    tokenRepository.save(temporary_token);
                }
                Long code = Long.valueOf(new Random().nextInt(((999999-100000)+1)+100000));
                mailService.sendText(email, String.valueOf(code));

                Verify verify = new Verify();
                verify.setCode(code);
                verify.setUser_id(user.get().getId());

                verifyRepository.save(verify);

                return new ApiResponse("Parolni qayta tiklash uchun kod emailingizga yuborildi", true, token);

            } else return new ApiResponse("email xato", false);
        } catch (Exception e) {
            e.printStackTrace();
            return new ApiResponse("some error", false);
        }
    }

    @Transactional
    public ApiResponse forgetPasswordConfirm(String code) {
        try {
            String token = JwtFilter.getToken;
            if (jwtProvider.validateToken(token)) {
                String email = jwtProvider.getUsername(token);
                Optional<User> optionalUser = userRepository.findByEmail(email);
                if (optionalUser.isPresent()) {
                    User user = optionalUser.get();
                    Token db_token = tokenRepository.findByToken(token).get();
                    if (db_token.getForr().equals("FORGET_PASSWORD")) {
                        Verify verify = verifyRepository.getByUserId(user.getId()).get();
                        if (String.valueOf(verify.getCode()).equals(code)) {
                            return new ApiResponse("code correct", true, token);
                        }
                        return new ApiResponse("code incorrect", false);
                    }
                    return new ApiResponse("token error", false);
                }
                return new ApiResponse("user not found", false);
            }
            return new ApiResponse("token error", false);
        } catch (Exception e) {
            e.printStackTrace();
            return new ApiResponse("some error", false);
        }
    }

    @Transactional
    public ApiResponse setNewPassword(String confirmCode, String newPassword) {
        try {
            String token = JwtFilter.getToken;
            Optional<Token> optionalToken = tokenRepository.findByToken(token);
            Token tokenPresent = optionalToken.isPresent() ? optionalToken.get() : null;
            if (tokenPresent != null && jwtProvider.validateToken(token) && tokenPresent.getForr().equals("FORGET_PASSWORD")) {
                String email = jwtProvider.getUsername(token);
                User db_user = userRepository.findByEmail(email).get();
                Verify verify = verifyRepository.getByUserId(db_user.getId()).get();
                if(String.valueOf(verify.getCode()).equals(confirmCode)) {
                    db_user.setPassword(passwordEncoder.encode(newPassword));
                    userRepository.save(db_user);
                    verifyRepository.delete(verify);
                    tokenRepository.deleteById(tokenPresent.getId());
                    return new ApiResponse("Parol muvoffaqiyatli yangilandi. Ilovaga qaytib akkountingizga kirishingiz mumkin", true);
                }
                return new ApiResponse("confirm code incorrect", false);
            }
            return new ApiResponse("Token eskirgan. Iltimos amaliyotni qayta bajaring", false);
        } catch (Exception e) {
            e.printStackTrace();
            return new ApiResponse(e.getMessage(), false);
        }
    }

    @Transactional
    public ApiResponse registerConfirm(String code) {
        try {
            String token = JwtFilter.getToken;
            if (jwtProvider.validateToken(token)) {
                String email = jwtProvider.getUsername(token);
                Optional<User> optionalUser = userRepository.findByEmail(email);
                if (optionalUser.isPresent()) {
                    User user = optionalUser.get();
                    Token db_token = tokenRepository.findByToken(token).get();
                    if (db_token.getForr().equals("REGISTER_CONFIRM")) {
                        Verify verify = verifyRepository.getByUserId(user.getId()).get();
                        if (String.valueOf(verify.getCode()).equals(code)) {
                            tokenRepository.delete(db_token);
                            verifyRepository.delete(verify);
                            user.setVerified(true);
                            userRepository.save(user);
                            return new ApiResponse("Register success", true);
                        }
                        return new ApiResponse("code incorrect", false);
                    }
                    return new ApiResponse("token error", false);
                }
                return new ApiResponse("user not found", false);
            }
            return new ApiResponse("token error", false);
        } catch (Exception e) {
            e.printStackTrace();
            return new ApiResponse("some error", false);
        }
    }

    public ApiResponse signUp(RegisterDto dto) {
        try {

            Optional<User> userOptional = userRepository.findByEmail(dto.getEmail());

            if (userOptional.isPresent()) {
                if (userOptional.get().isEnabled() && userOptional.get().isVerified()) {
                    return new ApiResponse("This user is already registered", false);
                }
                verifyRepository.delete(verifyRepository.getByUserId(userOptional.get().getId()).get());
                userRepository.delete(userOptional.get());
            }

            User user = new User();
            user.setFirstName(dto.getFirstName());
            user.setLastName(dto.getLastName());
            user.setPassword(passwordEncoder.encode(dto.getPassword()));
            user.setRoles(Set.of(roleRepository.findByName("USER").get()));
            user.setEnabled(true);
            user.setVerified(false);
            user.setAddress(dto.getAddress());
            user.setPhoneNumber(dto.getPhoneNumber());
            user.setEmail(dto.getEmail());
            user.setLegal(dto.isLegal());

            Token token = new Token();
            token.setType("ACCESS_TOKEN");
            token.setLevel("TEMPORARY");
            token.setForr("REGISTER_CONFIRM");

            String tokenForVerify = jwtProvider.generateTokenForVerify(dto.getEmail());

            token.setToken(tokenForVerify);
            token.setUser(user);
            user.setTokens(List.of(token));

            userRepository.save(user);

            sendingVerificationCodeToUser(dto.getEmail());

            return new ApiResponse("Tasdiqlash kodi emailingizga yuborildi!", true, token.getToken());
        } catch (Exception e) {
            e.printStackTrace();
            return new ApiResponse(e.getMessage(), false);
        }
    }

    public ApiResponse signUp2(RegisterDto dto) {
        try {

            Optional<User> userOptional = userRepository.findByEmail(dto.getEmail());

            if (userOptional.isPresent()) {
                if (userOptional.get().isEnabled() && userOptional.get().isVerified()) {
                    return new ApiResponse("This user is already registered", false);
                }
                userRepository.delete(userOptional.get());
            }

            User user = new User();
            user.setFirstName(dto.getFirstName());
            user.setLastName(dto.getLastName());
            user.setPassword(passwordEncoder.encode(dto.getPassword()));
            user.setRoles(Set.of(roleRepository.findByName("USER").get()));
            user.setEnabled(true);
            user.setVerified(true);
            user.setAddress(dto.getAddress());
            user.setPhoneNumber(dto.getPhoneNumber());
            user.setEmail(dto.getEmail());
            user.setLegal(dto.isLegal());

            userRepository.save(user);

            return new ApiResponse("success", true);
        } catch (Exception e) {
            e.printStackTrace();
            return new ApiResponse(e.getMessage(), false);
        }
    }

    public ApiResponse refreshToken(String refresh_token) {
        try {
            Optional<Token> db_refresh_token = tokenRepository.findByToken(refresh_token);
            if (db_refresh_token.isPresent() && jwtProvider.validateToken(refresh_token) && db_refresh_token.get().getType().equals("REFRESH_TOKEN")) {
                String email = jwtProvider.getUsername(refresh_token);
                Optional<User> optionalUser = userRepository.findByEmail(email);
                Token db_access_token = optionalUser.get().getTokens().stream().filter(t -> t.getForr().equals("ANY") && t.getType().equals("ACCESS_TOKEN")).findFirst().get();
                String a_token = jwtProvider.generateToken(email, optionalUser.get().getAuthorities(), "ACCESS_TOKEN");
                String r_token = jwtProvider.generateToken(email, optionalUser.get().getAuthorities(), "REFRESH_TOKEN");
                db_access_token.setToken(a_token);
                db_refresh_token.get().setToken(r_token);
                tokenRepository.save(db_access_token);
                tokenRepository.save(db_refresh_token.get());
                return new ApiResponse("access and refresh token successfully update", true, new AccessRefreshTokenDto(a_token, r_token));
            } else {
                return new ApiResponse("some error", false);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return new ApiResponse("failed", false);
        }
    }

    public ApiResponse confirmSignUp(Long code) {
        try {
            String email = JwtFilter.getEmail;
            Optional<User> userOptional = userRepository.findByEmail(email);
            if (userOptional.isPresent()) {
                User user = userOptional.get();
                Optional<Verify> verifyOptional = verifyRepository.getByUserId(user.getId());
                if (verifyOptional.isPresent()) {
                    Verify verify = verifyOptional.get();
                    if (verify.getCode().equals(code)) {
                        verifyRepository.delete(verifyOptional.get());
                        user.setEnabled(true);
                        user.setVerified(true);
                        userRepository.save(user);
                        return new ApiResponse("Muvoffaqiyatli ro'yxatdan o'tdingiz", true);
                    } else {
                        return new ApiResponse("verify code is incorrect!", false);
                    }
                } else {
                    return new ApiResponse("some error", false);
                }
            } else {
                return new ApiResponse("user not found", false);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return new ApiResponse("failed", false);
        }
    }

    public ApiResponse getUserInfo() {

        try {
            String email = JwtFilter.getEmail;
            Optional<User> userOptional = userRepository.findByEmail(email);
            User user = userOptional.get();

            UserDto userDto = new UserDto();
            userDto.setId(user.getId());
            userDto.setFirstName(user.getFirstName());
            userDto.setLastName(user.getLastName());
            userDto.setPhoneNumber(user.getPhoneNumber());
            userDto.setAddress(user.getAddress());
            userDto.setEmail(user.getEmail());

            return new ApiResponse("success", true, userDto);

        } catch (Exception e) {
            e.printStackTrace();
            return new ApiResponse("failed", false);
        }
    }

    public ApiResponse getUsersInfos() {
        try {
            List<User> users = userRepository.findAll();
            List<UserDto> userDtoList = new ArrayList<>();
            for (int i = 0; i < users.size(); i++) {

                User user = users.get(i);
                UserDto userDto = new UserDto();
                userDto.setId(user.getId());
                userDto.setFirstName(user.getFirstName());
                userDto.setLastName(user.getLastName());
                userDto.setPhoneNumber(user.getPhoneNumber());
                userDto.setAddress(user.getAddress());
                userDto.setEmail(user.getEmail());

                userDtoList.add(userDto);

            }

            return new ApiResponse("success", true, userDtoList);
        } catch (Exception e) {
            e.printStackTrace();
            return new ApiResponse("some erro", false);
        }
    }

    public ApiResponse addProduct(String productName, double weight, String branch, String department, MultipartFile multipartFile) {
        try {

//            return new ApiResponse("productName:" + productName + ", weight:" + weight, true, multipartFile.getName() + ", " + multipartFile.getOriginalFilename() + ", " + multipartFile.getSize() + ", " + multipartFile.getContentType());

            if (productRepository.checkProductName(productName)) {
                String path = saveImageInfo(multipartFile);
                ProductHistory productHistory = new ProductHistory();
                productHistory.setName(productName);
                productHistory.setWeight(weight);
                productHistory.setImg_path(path);
                productHistory.set_active(true);
                productHistory.setCreated_date(LocalDate.now());
                productHistory.setBranch(branch);
                productHistory.setDepartment(department);
                productHistory.setUser(userRepository.findByEmail(JwtFilter.getEmail).get());
                productHistoryRepository.save(productHistory);
                return new ApiResponse("success", true);
            } else {
                return new ApiResponse("Bunday maxsulot bazada topilmadi", false);
            }

        } catch (Exception e) {
            e.printStackTrace();
            return new ApiResponse("Image size or format exception", false);
        }
    }

    public ApiResponse getAllProductHistory() {
        try {
            return new ApiResponse("success", true, productHistoryRepository.getAll());
        } catch (Exception e) {
            e.printStackTrace();
            return new ApiResponse("failed", false);
        }
    }

    public ApiResponse getAllProductList() {
        try {
            return new ApiResponse("success", true, productRepository.findAll());
        } catch (Exception e) {
            e.printStackTrace();
            return new ApiResponse("failed", false);
        }
    }

    public ApiResponse addProduct(Product product) {
        try {
            if (productRepository.checkProductName(product.getName())) {
                return new ApiResponse("Bunday turdagi maxsulot mavjud", false);
            } else {
                productRepository.save(product);
                return new ApiResponse("success", true);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return new ApiResponse("failed", false);
        }
    }

    public ApiResponse editProduct(Product product) {
        try {
            if (productRepository.checkProductName(product.getName())) {
                productRepository.save(product);
                return new ApiResponse("Muvoffaqiyatli o'zgartirildi", true);
            } else {
                return new ApiResponse("Bunday turdagi maxsulot mavjud emas", false);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return new ApiResponse("failed", false);
        }
    }

    public String saveImageInfo(MultipartFile multipartFile) throws IOException {

//        String imagesFolder = "src/main/resources/images";

        UUID uuid = UUID.randomUUID();
        String originalFilename = multipartFile.getOriginalFilename();
        String fileType = originalFilename.substring(originalFilename.lastIndexOf("."));
        String fileName = uuid.toString() + fileType;

        Path filePath = Paths.get(path, fileName);

        if (!Files.exists(filePath.getParent())) {
            Files.createDirectories(filePath.getParent());
        }

        Files.copy(multipartFile.getInputStream(), filePath);

        return d_path + fileName;
    }

    public ApiResponse salom() {
        String getRole = JwtFilter.getRole;
        String message = "";
        if (getRole.equals("HR")) {
            message = "salom HR";
        } else if (getRole.equals("EMPLOYEE")) {
            message = "salom employee";
        }
        return new ApiResponse(message, true);
    }

    public ApiResponse sendingVerificationCodeToUser(String email) {
        try {
            Optional<User> userOptional = userRepository.findByEmail(email);
            User user = userOptional.get();
            Optional<Verify> verifyOptional = verifyRepository.getByUserId(user.getId());
            if (verifyOptional.isPresent()) {
                Verify verify = verifyOptional.get();
                verifyRepository.delete(verify);
            }

            Long code = Long.valueOf(new Random().nextInt((999999-100000)+1)+100000);

            ApiResponse apiResponse = mailService.sendText(email, String.valueOf(code));

            Verify verify = new Verify();
            verify.setUser_id(user.getId());
            verify.setCode(code);
            verifyRepository.save(verify);

            return apiResponse;

        } catch (Exception e) {
            e.printStackTrace();
            return new ApiResponse("some error", false);
        }
    }

    public ApiResponse getProductHistory() {
        try {
            String email = JwtFilter.getEmail;
            return new ApiResponse("success", true, productHistoryRepository.getByUserId(userRepository.findByEmail(email).get().getId()));
        } catch (Exception e) {
            e.printStackTrace();
            return new ApiResponse("some error", false);
        }
    }

//    public ApiResponse getAllBranch() {
//        try {
//            return new ApiResponse("success", true, branchRepository.findAll());
//        } catch (Exception e) {
//            e.printStackTrace();
//            return new ApiResponse("some error", false);
//        }
//    }
//
//    public ApiResponse createBranch(String branchName) {
//        try {
//            Optional<Branch> optionalBranch = branchRepository.findByName(branchName);
//            if (optionalBranch.isPresent()) {
//                return new ApiResponse("branch elready exicts.", false);
//            } else {
//                Branch branch = new Branch();
//                branch.setName(branchName);
//                return new ApiResponse("branch success created", true, branchRepository.save(branch));
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//            return new ApiResponse("some error", false);
//        }
//    }
//
//    public ApiResponse deleteBranch(String branchName) {
//        try {
//            Optional<Branch> optionalBranch = branchRepository.findByName(branchName);
//            if (optionalBranch.isPresent()) {
//                branchRepository.deleteById(optionalBranch.get().getId());
//                return new ApiResponse("branch successfully deleted.", true);
//            } else {
//                return new ApiResponse("branch name not found.", false);
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//            return new ApiResponse("some error", false);
//        }
//    }
//
//    public ApiResponse addDepartment(String branchName, String departmentName) {
//        try {
//            Optional<Branch> optionalBranch = branchRepository.findByName(branchName);
//            Optional<Department> optionalDepartment = departmentRepository.findByName(departmentName);
//            if (optionalBranch.isPresent()) {
//                if (optionalDepartment.isPresent()) {
//                    return new ApiResponse("branch elready exicts.", false);
//                } else {
//                    Department department = new Department();
//                    department.setName(departmentName);
//                    department.setBranch(optionalBranch.get());
//                    optionalBranch.get().getDepartments().add(departmentRepository.save(department));
//                    branchRepository.save(optionalBranch.get());
//                    return new ApiResponse("department successfully added to branch", true, optionalBranch.get());
//                }
//            } else {
//                return new ApiResponse("branch name not found", false);
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//            return new ApiResponse("some error", false);
//        }
//    }
//
//    public ApiResponse deleteDepartment(String branchName, String departmentName) {
//        try {
//            Optional<Branch> optionalBranch = branchRepository.findByName(branchName);
//            Optional<Department> optionalDepartment = departmentRepository.findByName(departmentName);
//            if (optionalBranch.isPresent()) {
//                if (optionalDepartment.isPresent()) {
//                    departmentRepository.deleteById(optionalDepartment.get().getId());
//                    return new ApiResponse("department successfully deleted.", true);
//                } else {
//                    return new ApiResponse("department name not found in this branch", false);
//                }
//            } else {
//                return new ApiResponse("branch name not found", false);
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//            return new ApiResponse("some error", false);
//        }
//    }
}