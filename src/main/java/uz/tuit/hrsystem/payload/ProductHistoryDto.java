package uz.tuit.hrsystem.payload;

import java.time.LocalDate;

public interface ProductHistoryDto {
    Long getUser_id();
    String getProduct_img_path();
    String getFirst_name();
    String getLast_name();
    String getAddress();
    String getPhone_number();
    String getName();
    Double getWeight();
    String getBranch();
    String getDepartment();
    LocalDate getCreated_date();
}
