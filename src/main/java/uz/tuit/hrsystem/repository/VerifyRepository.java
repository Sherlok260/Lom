package uz.tuit.hrsystem.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import uz.tuit.hrsystem.entity.Verify;

import java.util.Optional;

public interface VerifyRepository extends JpaRepository<Verify, Long> {
    @Query(value = "select * from verify where user_id=?1", nativeQuery = true)
    Optional<Verify> getByUserId(Long user_id);
}
