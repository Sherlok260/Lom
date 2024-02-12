package uz.tuit.hrsystem.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import uz.tuit.hrsystem.entity.User;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    @Query(value = "select * from users where phone_number=?1", nativeQuery = true)
    Optional<User> findByPhoneNumber(String phoneNumber);

    @Query(value = "select * from users where email=?1", nativeQuery = true)
    Optional<User> findByEmail(String email);
}
