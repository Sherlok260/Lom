package uz.tuit.hrsystem.component;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import uz.tuit.hrsystem.entity.Product;
import uz.tuit.hrsystem.entity.Role;
import uz.tuit.hrsystem.entity.User;
import uz.tuit.hrsystem.jwt.JwtProvider;
import uz.tuit.hrsystem.payload.RegisterDto;
import uz.tuit.hrsystem.repository.ProductRepository;
import uz.tuit.hrsystem.repository.RoleRepository;
import uz.tuit.hrsystem.repository.UserRepository;
import uz.tuit.hrsystem.repository.TokenRepository;
import uz.tuit.hrsystem.service.UserService;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Set;

@Component
public class DataLoader implements CommandLineRunner {

    @Autowired
    UserService userService;

    @Autowired
    ProductRepository productRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    TokenRepository tokenRepository;

    @Autowired
    JwtProvider jwtProvider;

    @Autowired
    RoleRepository roleRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Value("${spring.sql.init.mode}")
    private String initMode;


    public void getServerIp() throws UnknownHostException {
        InetAddress address = InetAddress.getLocalHost();
        String ipAddress = address.getHostAddress();

        System.out.println("Server IP Address: " + ipAddress);
    }

    @Override
    public void run(String... args) throws UnknownHostException {
        getServerIp();
        if (initMode.equals("always")) {
            if (userRepository.count() == 0) {

                Role userRole = new Role();
                userRole.setName("USER");
                Role user_role = roleRepository.save(userRole);

                Role adminRole = new Role();
                adminRole.setName("ADMIN");
                Role admin_role = roleRepository.save(adminRole);


                User admin = new User();
                admin.setFirstName("Admin");
                admin.setLastName("Adminov");
                admin.setPassword(passwordEncoder.encode("adminman"));
                admin.setPhoneNumber("+998123456789");
                admin.setEnabled(true);
                admin.setVerified(true);
                admin.setRoles(Set.of(admin_role));

                userRepository.save(admin);

                userService.signUp2(new RegisterDto("Shaxzod", "Murtozaqulov", "+998939320618", "Toshkent", "8600312912121212", "shaxzod123", false));
                userService.signUp2(new RegisterDto("Oybek", "Jumanov", "+998956558989", "Toshkent","8600312912121212", "oybek123", false));

                Product product = new Product();
                product.setName("Mis");
                product.setPrice(2000);

                userService.addProduct(product);

            }
        }
    }
}
