## Project Structure

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
    challenge -.-> challenge_sub["(controller, service, repository,\ndto, domain, enumType, exception)"]
    challenge_sub:::subtext

    %% cloudinary 패키지
    pkg --> cloudinary["cloudinary/"]
    cloudinary -.-> cloudinary_sub["(service, dto, exception)"]
    cloudinary_sub:::subtext

    %% notification 패키지
    pkg --> notification["notification/"]
    notification -.-> notification_sub["(controller, service, repository,\ndomain, enumType)"]
    notification_sub:::subtext

    %% oauth 패키지
    pkg --> oauth["oauth/"]
    oauth -.-> oauth_sub["(controller, service, dto, exception)"]
    oauth_sub:::subtext

    %% post 패키지
    pkg --> post["post/"]
    post -.-> post_sub["(controller, service, repository,\ndomain, dto, exception)"]
    post_sub:::subtext

    %% scheduler 패키지
    pkg --> scheduler["scheduler/"]

    %% tag 패키지
    pkg --> tag["tag/"]
    tag -.-> tag_sub["(controller, service, repository,\ndomain, dto, exception)"]
    tag_sub:::subtext

    %% token 패키지
    pkg --> token["token/"]
    token -.-> token_sub["(controller, service, repository,\ndomain, dto, exception)"]
    token_sub:::subtext

    %% user 패키지
    pkg --> user["user/"]
    user -.-> user_sub["(controller, service, repository,\ndomain, dto, enumType, exception)"]
    user_sub:::subtext

    %% view 패키지
    pkg --> view["view/"]
    view -.-> view_sub["(controller, service, dto)"]
    view_sub:::subtext

    %% 스타일 정의
    classDef subtext fill:transparent,stroke:transparent,color:#666,font-size:10px

    %% config 하위 패키지
    %% jwt 패키지
    config --> jwt_pkg["jwt/"]

    %% oauth 패키지
    config --> oauth_pkg["oauth/"]

    %% cloudinary 패키지
    config --> cloudinary_pkg["cloudinary/"]