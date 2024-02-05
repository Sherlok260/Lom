package uz.tuit.hrsystem.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.security.authentication.AuthenticationManager;
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
import org.springframework.core.io.UrlResource;

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
    AuthenticationManagerBuilder authenticationManagerBuilder;

    @Transactional
    public ApiResponse login(LoginDto dto) {
        try {

            Authentication authentication = authenticationManagerBuilder.getObject().authenticate(new UsernamePasswordAuthenticationToken(
                    dto.getPhoneNumber(),
                    dto.getPassword()
            ));
            SecurityContextHolder.getContext().setAuthentication(authentication);

            User user = userRepository.findByPhoneNumber(dto.getPhoneNumber()).get();

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
            return new ApiResponse("phoneNumber or password incorrect", false);
        }
    }

    @Transactional
    public ApiResponse logout() {
        try {
            String phoneNumber = JwtFilter.getphoneNumber;
            User user = userRepository.findByPhoneNumber(phoneNumber).get();
//            tokenRepository.deleteAll(user.getTokens());
            tokenRepository.deleteByUserId(user.getId());
            return new ApiResponse("success logout", true);
        } catch (Exception e) {
            e.printStackTrace();
            return new ApiResponse(e.getMessage(), false);
        }
    }

    public ApiResponse signUp(RegisterDto dto) {
        try {

            Optional<User> userOptional = userRepository.findByPhoneNumber(dto.getPhoneNumber());

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
            user.setVerified(false);
            user.setAddress(dto.getAddress());
            user.setPhoneNumber(dto.getPhoneNumber());
            user.setLegal(dto.isLegal());

            Token token = new Token();
            token.setType("ACCESS_TOKEN");
            token.setLevel("TEMPORARY");
            token.setForr("REGISTER_CONFIRM");

            String tokenForVerify = jwtProvider.generateTokenForVerify(dto.getPhoneNumber());

            token.setToken(tokenForVerify);
            token.setUser(user);
            user.setTokens(List.of(token));

            sendingVerificationCodeToUser(dto.getPhoneNumber());

            userRepository.save(user);

            return new ApiResponse("Tasdiqlash kodi raqamingizga yuborildi!", true, token.getToken());
        } catch (Exception e) {
            e.printStackTrace();
            return new ApiResponse(e.getMessage(), false);
        }
    }


    public ApiResponse signUp2(RegisterDto dto) {
        try {

            Optional<User> userOptional = userRepository.findByPhoneNumber(dto.getPhoneNumber());

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
            if(db_refresh_token.isPresent() && jwtProvider.validateToken(refresh_token) && db_refresh_token.get().getType().equals("REFRESH_TOKEN")) {
                String phoneNumber = jwtProvider.getUsername(refresh_token);
                Optional<User> optionalUser = userRepository.findByPhoneNumber(phoneNumber);
                Token db_access_token = optionalUser.get().getTokens().stream().filter(t -> t.getForr().equals("ANY") && t.getType().equals("ACCESS_TOKEN")).findFirst().get();
                String a_token = jwtProvider.generateToken(phoneNumber, optionalUser.get().getAuthorities(), "ACCESS_TOKEN");
                String r_token = jwtProvider.generateToken(phoneNumber, optionalUser.get().getAuthorities(), "REFRESH_TOKEN");
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
            String phoneNumber = JwtFilter.getphoneNumber;
            Optional<User> userOptional = userRepository.findByPhoneNumber(phoneNumber);
            if(userOptional.isPresent()) {
                User user = userOptional.get();
                Optional<Verify> verifyOptional = verifyRepository.getByUserId(user.getId());
                if(verifyOptional.isPresent()) {
                    Verify verify = verifyOptional.get();
                    if(verify.getCode().equals(code)) {
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
            String phoneNumber = JwtFilter.getphoneNumber;
            Optional<User> userOptional = userRepository.findByPhoneNumber(phoneNumber);
            User user = userOptional.get();

            UserDto userDto = new UserDto();
            userDto.setId(user.getId());
            userDto.setFirstName(user.getFirstName());
            userDto.setLastName(user.getLastName());
            userDto.setPhoneNumber(user.getPhoneNumber());
            userDto.setAddress(user.getAddress());

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

                userDtoList.add(userDto);

            }

            return new ApiResponse("success", true, userDtoList);
        } catch (Exception e) {
            e.printStackTrace();
            return new ApiResponse("some erro", false);
        }
    }

    public ApiResponse addProduct(String productName, double weight, MultipartFile multipartFile) {
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
                productHistory.setUser(userRepository.findByPhoneNumber(JwtFilter.getphoneNumber).get());
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

    public Resource getProductImg(Long product_id) {
        try {
            String img_path = productHistoryRepository.findById(product_id).get().getImg_path();
            Path dirPath = Paths.get("src/main/resources/" + img_path);
            return new UrlResource(dirPath.toUri());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
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

    public void sendingVerificationCodeToUser(String phoneNumber) {
        Optional<User> userOptional = userRepository.findByPhoneNumber(phoneNumber);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            Optional<Verify> verifyOptional = verifyRepository.getByUserId(user.getId());
            if (verifyOptional.isPresent()) {
                Verify verify = verifyOptional.get();
                verifyRepository.delete(verify);
            }
            Verify verify = new Verify();
            verify.setUser_id(user.getId());
            verify.setCode(1111L);
            verifyRepository.save(verify);
        }
    }

    public ApiResponse getProductHistory() {
        try {
            String phoneNumber = JwtFilter.getphoneNumber;
            return new ApiResponse("success", true, productHistoryRepository.getByUserId(userRepository.findByPhoneNumber(phoneNumber).get().getId()));
        } catch (Exception e) {
            e.printStackTrace();
            return new ApiResponse("some error", false);
        }
    }
}