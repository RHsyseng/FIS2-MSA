#! /bin/bash

mkdir yaml-templates
cd yaml-templates
curl -O -L https://raw.githubusercontent.com/RHsyseng/FIS2-MSA/master/yaml-templates/logging-deployer.yaml
curl -O -L https://raw.githubusercontent.com/RHsyseng/FIS2-MSA/master/yaml-templates/logging-accounts.sh
chmod +x logging-accounts.sh
curl -O -L https://raw.githubusercontent.com/RHsyseng/FIS2-MSA/master/yaml-templates/billing-template.yaml
curl -O -L https://raw.githubusercontent.com/RHsyseng/FIS2-MSA/master/yaml-templates/gateway-template.yaml
curl -O -L https://raw.githubusercontent.com/RHsyseng/FIS2-MSA/master/yaml-templates/messaging-template.yaml
curl -O -L https://raw.githubusercontent.com/RHsyseng/FIS2-MSA/master/yaml-templates/presentation-template.yaml
curl -O -L https://raw.githubusercontent.com/RHsyseng/FIS2-MSA/master/yaml-templates/product-template.yaml
curl -O -L https://raw.githubusercontent.com/RHsyseng/FIS2-MSA/master/yaml-templates/sales-template.yaml
curl -O -L https://raw.githubusercontent.com/RHsyseng/FIS2-MSA/master/yaml-templates/warehouse-template.yaml
