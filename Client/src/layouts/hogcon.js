import { Layout } from "antd";
import HogContest from "../components/hogcon/contest";

const HogContestLayout = () => {
  return (
    <Layout
      style={{
        paddingTop: "10vh",
        paddingBottom: "10vh",
        paddingLeft: "10vw",
        paddingRight: "10vw",
      }}
    >
      <HogContest />
    </Layout>
  );
};

export default HogContestLayout;
