import {useSelector} from "react-redux";

const Welcome = () => {
    const auth = useSelector((state) => state.auth.value);
    const username = auth?.username;
    const fullName = auth?.fullName;

    return (
        <p>欢迎访问SICP Online Judge，您已经以{fullName}（{username}）的身份登录。</p>
    );
};

export default Welcome;
