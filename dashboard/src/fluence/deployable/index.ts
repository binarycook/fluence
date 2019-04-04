import {TransactionReceipt} from "web3/types";
import {web3js} from "../contract";
import {account, defaultContractAddress} from "../../constants";
import {AppId} from "../apps";
import {APP_DEPLOY_FAILED, APP_DEPLOYED, APP_ENQUEUED} from "../../front/actions/deployable/deploy";
import abi from '../../abi/Network.json';

import { parseLog } from "ethereum-event-logs"

export enum StorageType {
    Swarm = 0,
    Ipfs = 1
}

export type DeployableAppId = string;

export interface DeployableApp {
    name: string,
    shortName: string,
    storageHash: string,
    storageType: StorageType,
    clusterSize: number,
}

export const deployableAppIds: [DeployableAppId] = ["llamadb"];

export const deployableApps: { [key: string]: DeployableApp } = {
    "llamadb": {
        name: "SQL DB (llamadb)",
        shortName: "SQL DB",
        storageHash: "0x090A9B7CCA9D55A9632BBCC3A30A57F2DB1D1FD688659CFF95AB8D1F904AD74B",
        storageType: StorageType.Ipfs,
        clusterSize: 4
    }
};

// Sends a signed transaction to Ethereum
export function send(signedTx: Buffer): Promise<TransactionReceipt> {
    return web3js
        .eth
        .sendSignedTransaction('0x' + signedTx.toString('hex'))
        .once("transactionHash", h => {
            console.log("tx hash " + h)
        });
}

// Builds TxParams object to later use for building a transaction
export async function txParams(txData: string): Promise<any> {
    let nonce = web3js.utils.numberToHex(await web3js.eth.getTransactionCount(account, "pending"));
    let gasPrice = web3js.utils.numberToHex(await web3js.eth.getGasPrice());
    let gasLimit = web3js.utils.numberToHex(1000000);
    return {
        nonce: nonce,
        gasPrice: gasPrice,
        gasLimit: gasLimit,
        to: defaultContractAddress,
        value: '0x00',
        data: txData,
        // EIP 155 chainId - mainnet: 1, rinkeby: 4
        chainId: 4
    };
}

// Parse AppDeployed or AppEnqueued from TransactionReceipt
export function checkLogs(receipt: TransactionReceipt): [string, AppId | undefined] {
    type AppEvent = { name: string, args: { appID: AppId } }
    let logs: AppEvent[] = parseLog(receipt.logs, abi);
    let enqueued = logs.find(l => l.name == "AppEnqueued");
    let deployed = logs.find(l => l.name == "AppDeployed");
    if (enqueued != undefined) {
        console.log("App enqueued with appID = " + enqueued.args.appID);
        return [APP_ENQUEUED, enqueued.args.appID];
    } else if (deployed != undefined) {
        console.log("App deployed with appID = " + deployed.args.appID);
        return [APP_DEPLOYED, deployed.args.appID];
    }

    console.error("No AppDeployed or AppEnqueued event in logs: " + JSON.stringify(logs));
    return [APP_DEPLOY_FAILED, undefined];
}