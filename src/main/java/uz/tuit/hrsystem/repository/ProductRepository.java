package uz.tuit.hrsystem.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import uz.tuit.hrsystem.entity.Product;

public interface ProductRepository extends JpaRepository<Product, Long> {
    @Query(value = "select exists (select 1 from product where name=?1);", nativeQuery = true)
    boolean checkProductName(String name);
}
