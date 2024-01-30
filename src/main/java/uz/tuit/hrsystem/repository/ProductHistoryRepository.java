package uz.tuit.hrsystem.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import uz.tuit.hrsystem.entity.ProductHistory;
import uz.tuit.hrsystem.payload.ProductHistoryDto;

import java.util.List;

public interface ProductHistoryRepository extends JpaRepository<ProductHistory, Long> {

    @Query(value = "select u.id as user_id, p.img_path as product_img_path, u.first_name, u.last_name, u.address, u.phone_number, p.name, p.weight, p.created_date from product_history p left join users u on u.id=p.user_id order by p.created_date desc", nativeQuery = true)
    List<ProductHistoryDto> getAll();

}