package uz.tuit.hrsystem.payload;

import java.time.LocalDate;

public interface ProductHistoryDto {
    Long getUser_id();
    Long getProduct_img_id();
    String getFirst_name();
    String getLast_name();
    String getAddress();
    String getPhone_number();
    String getName();
    Double getWeight();
    LocalDate getCreated_date();
}
