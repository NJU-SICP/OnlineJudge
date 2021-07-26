import React, {useEffect, useState} from "react";
import http from "../../http";
import {Card, Col, Row, Skeleton} from "antd";

const SubmissionTable = ({submissions}) => {
    const [selectedId, setSelectedId] = useState(null);
    const [submission, setSubmission] = useState(null);

    useEffect(() => {
        if (selectedId === null) {
            setSubmission(null);
        } else {
            http()
                .get(`/repositories/submissions/${selectedId}`)
                .then((res) => setSubmission(res.data))
                .catch((err) => console.error(err));
        }
    }, [selectedId])

    return (
        <>
            {submissions === null
                ? <Skeleton/>
                : <>
                    <Row>
                        <Col span={8}>
                            {submissions.map((s) => (
                                <Card key={s.Id}>
                                    wwwwww
                                </Card>
                            ))}
                        </Col>
                    </Row>
                </>}
        </>
    )
};

export default SubmissionTable;
