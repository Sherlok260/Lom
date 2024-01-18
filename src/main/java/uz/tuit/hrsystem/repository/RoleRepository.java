package uz.tuit.hrsystem.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uz.tuit.hrsystem.entity.Role;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(String name);
}
