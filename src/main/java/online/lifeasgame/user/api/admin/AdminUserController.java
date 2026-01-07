package online.lifeasgame.user.api.admin;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.user.api.admin.spec.AdminUserApiSpecV1;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/v1/users")
public class AdminUserController implements AdminUserApiSpecV1 {


}
