## Project Structure

![structure](repo-diagram.svg)

```mermaid
flowchart TD
    root["BackEnd/"]

    root --> src["src/"]
    src --> main["main/"]
    main --> java["java/"]
    java --> pkg["me/jsu/springbootdeveloper/"]

    %% config 패키지
    pkg --> config["config/"]

    %% common 패키지
    pkg --> common["common/"]

    %% challenge 패키지
    pkg --> challenge["challenge/"]

    %% cloudinary 패키지
    pkg --> cloudinary["cloudinary/"]

    %% notification 패키지
    pkg --> notification["notification/"]

    %% oauth 패키지
    pkg --> oauth["oauth/"]

    %% post 패키지
    pkg --> post["post/"]

    %% scheduler 패키지
    pkg --> scheduler["scheduler/"]

    %% tag 패키지
    pkg --> tag["tag/"]

    %% token 패키지
    pkg --> token["token/"]

    %% user 패키지
    pkg --> user["user/"]

    %% view 패키지
    pkg --> view["view/"]

    %% ============================
    %% 도메인별 하위 패키지
    %% ============================

    %% challenge 하위 패키지
    %% challenge.repository 패키지
    challenge --> challenge_repository["repository/"]
    %% challenge.service 패키지
    challenge --> challenge_service["service/"]
    %% challenge.controller 패키지
    challenge --> challenge_controller["controller/"]
    %% challenge.dto 패키지
    challenge --> challenge_dto["dto/"]
    %% challenge.domain 패키지
    challenge --> challenge_domain["domain/"]
    %% challenge.enumType 패키지
    challenge --> challenge_enumType["enumType/"]
    %% challenge.exception 패키지
    challenge --> challenge_exception["exception/"]

    %% cloudinary 하위 패키지
    %% cloudinary.service 패키지
    cloudinary --> cloudinary_service["service/"]
    %% cloudinary.dto 패키지
    cloudinary --> cloudinary_dto["dto/"]
    %% cloudinary.exception 패키지
    cloudinary --> cloudinary_exception["exception/"]

    %% notification 하위 패키지
    %% notification.controller 패키지
    notification --> notification_controller["controller/"]
    %% notification.service 패키지
    notification --> notification_service["service/"]
    %% notification.repository 패키지
    notification --> notification_repository["repository/"]
    %% notification.domain 패키지
    notification --> notification_domain["domain/"]
    %% notification.enumType 패키지
    notification --> notification_enumType["enumType/"]

    %% oauth 하위 패키지
    %% oauth.controller 패키지
    oauth --> oauth_controller["controller/"]
    %% oauth.service 패키지
    oauth --> oauth_service["service/"]
    %% oauth.dto 패키지
    oauth --> oauth_dto["dto/"]
    %% oauth.exception 패키지
    oauth --> oauth_exception["exception/"]

    %% post 하위 패키지
    %% post.controller 패키지
    post --> post_controller["controller/"]
    %% post.service 패키지
    post --> post_service["service/"]
    %% post.repository 패키지
    post --> post_repository["repository/"]
    %% post.domain 패키지
    post --> post_domain["domain/"]
    %% post.dto 패키지
    post --> post_dto["dto/"]
    %% post.exception 패키지
    post --> post_exception["exception/"]

    %% scheduler 하위 패키지
    %% (현재 scheduler는 클래스 하나이므로 세부 패키지 없이 scheduler/만 표현)

    %% tag 하위 패키지
    %% tag.controller 패키지
    tag --> tag_controller["controller/"]
    %% tag.service 패키지
    tag --> tag_service["service/"]
    %% tag.repository 패키지
    tag --> tag_repository["repository/"]
    %% tag.domain 패키지
    tag --> tag_domain["domain/"]
    %% tag.dto 패키지
    tag --> tag_dto["dto/"]
    %% tag.exception 패키지
    tag --> tag_exception["exception/"]

    %% token 하위 패키지
    %% token.controller 패키지
    token --> token_controller["controller/"]
    %% token.service 패키지
    token --> token_service["service/"]
    %% token.repository 패키지
    token --> token_repository["repository/"]
    %% token.domain 패키지
    token --> token_domain["domain/"]
    %% token.dto 패키지
    token --> token_dto["dto/"]
    %% token.exception 패키지
    token --> token_exception["exception/"]

    %% user 하위 패키지
    %% user.controller 패키지
    user --> user_controller["controller/"]
    %% user.service 패키지
    user --> user_service["service/"]
    %% user.repository 패키지
    user --> user_repository["repository/"]
    %% user.domain 패키지
    user --> user_domain["domain/"]
    %% user.dto 패키지
    user --> user_dto["dto/"]
    %% user.enumType 패키지
    user --> user_enumType["enumType/"]
    %% user.exception 패키지
    user --> user_exception["exception/"]

    %% view 하위 패키지
    %% view.controller 패키지
    view --> view_controller["controller/"]
    %% view.service 패키지
    view --> view_service["service/"]
    %% view.dto 패키지
    view --> view_dto["dto/"]

    %% config 하위 패키지
    %% jwt 패키지
    config --> jwt_pkg["jwt/"]

    %% oauth 패키지
    config --> oauth_pkg["oauth/"]

    %% cloudinary 패키지
    config --> cloudinary_pkg["cloudinary/"]