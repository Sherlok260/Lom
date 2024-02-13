package uz.tuit.hrsystem.component;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import uz.tuit.hrsystem.entity.*;
import uz.tuit.hrsystem.jwt.JwtProvider;
import uz.tuit.hrsystem.payload.RegisterDto;
import uz.tuit.hrsystem.repository.*;
import uz.tuit.hrsystem.service.UserService;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
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

//    @Autowired
//    BranchRepository branchRepository;

//    @Autowired
//    DepartmentRepository departmentRepository;

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

                //add default Users and Admins
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
                admin.setEmail("admin@gmail.com");
                admin.setEnabled(true);
                admin.setVerified(true);
                admin.setRoles(Set.of(admin_role));
                userRepository.save(admin);

                userService.signUp2(new RegisterDto("Shaxzod", "Murtozaqulov", "+998939320618", "Toshkent", "shaxzod@gmail.com", "shaxzod123", false));

                //Add default product
                Product product = new Product();
                product.setName("Mis");
                product.setPrice(2000);
                userService.addProduct(product);

                //Add default branch and departments

//                Branch branch1 = new Branch();
//                Branch branch2 = new Branch();
//                Branch branch3 = new Branch();
//
//                Department department1_1 = new Department();
//                department1_1.setName("Department-1.1");
//                department1_1.setBranch(branch1);
//
//                Department department1_2 = new Department();
//                department1_2.setName("Department-1.2");
//                department1_2.setBranch(branch1);
//
//                Department department1_3 = new Department();
//                department1_3.setName("Department-1.3");
//                department1_3.setBranch(branch1);
//
//                Department department2_1 = new Department();
//                department2_1.setName("Department-2.1");
//                department2_1.setBranch(branch2);
//
//                Department department2_2 = new Department();
//                department2_2.setName("Department-2.2");
//                department2_2.setBranch(branch2);
//
//                Department department2_3 = new Department();
//                department2_3.setName("Department-2.3");
//                department2_3.setBranch(branch2);
//
//                Department department3_1 = new Department();
//                department3_1.setName("Department-3.1");
//                department3_1.setBranch(branch3);
//
//                Department department3_2 = new Department();
//                department3_2.setName("Department-3.2");
//                department3_2.setBranch(branch3);
//
//                Department department3_3 = new Department();
//                department3_3.setName("Department-3.3");
//                department3_3.setBranch(branch3);
//
//                branch1.setName("Branch1");
//                branch1.setDepartments(List.of(department1_1, department1_2, department1_3));
//
//                branch2.setName("Branch2");
//                branch2.setDepartments(List.of(department2_1, department2_2, department2_3));
//
//                branch3.setName("Branch3");
//                branch3.setDepartments(List.of(department3_1, department3_2, department3_3));
//
//                branchRepository.save(branch1);
//                branchRepository.save(branch2);
//                branchRepository.save(branch3);

            }
        }
    }
}
