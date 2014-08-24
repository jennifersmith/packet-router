set -e

aws iam get-user | grep ludumdares35



rm -rf published
mkdir published
cp index.html published/
cp packet_router.js published/
cp -r out published/
cp -r lib published/
aws s3 sync published s3://ludumdare30/
