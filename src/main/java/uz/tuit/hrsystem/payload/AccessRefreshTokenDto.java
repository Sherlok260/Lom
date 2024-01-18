package uz.tuit.hrsystem.payload;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class AccessRefreshTokenDto {
    private String access_token;
    private String refresh_token;
}
