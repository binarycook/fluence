/*
 * Copyright 2018 Fluence Labs Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fluence.effects.tendermint.block

import fluence.effects.tendermint.block.data.{Block, SimpleJsonCodecs}
import scodec.bits.ByteVector

object TestBlocks {

  val block1: String =
    """
      |{
      |  "header" : {
      |    "version" : {
      |      "block" : "10"
      |    },
      |    "chain_id" : "1",
      |    "height" : 1,
      |    "time" : "2019-10-10T14:34:38Z",
      |    "num_txs" : 0,
      |    "total_txs" : 0,
      |    "last_block_id" : {
      |      "parts" : {
      |
      |      }
      |    },
      |    "last_commit_hash" : "",
      |    "data_hash" : "",
      |    "validators_hash" : "aaf30643f81c4e3a2911e29db9a7a9ebb6712d3573031b0b119a5d233d531474",
      |    "next_validators_hash" : "aaf30643f81c4e3a2911e29db9a7a9ebb6712d3573031b0b119a5d233d531474",
      |    "consensus_hash" : "048091bc7ddc283f77bfbf91d73c44da58c3df8a9cbc867405d8b7f3daada22f",
      |    "app_hash" : "",
      |    "last_results_hash" : "",
      |    "evidence_hash" : "",
      |    "proposer_address" : "5a6efa0779fc0c143b120dafddfdede28d6b9ecc"
      |  },
      |  "data" : {
      |    "txs" : [
      |    ]
      |  },
      |  "last_commit" : {
      |    "block_id" : {
      |      "parts" : {
      |
      |      }
      |    },
      |    "precommits" : [
      |    ]
      |  }
      |}
      |""".stripMargin

  val block2: String =
    """
      |{
      |  "header" : {
      |    "version" : {
      |      "block" : "10"
      |    },
      |    "chain_id" : "1",
      |    "height" : 2,
      |    "time" : "2019-10-10T14:35:04.943960500Z",
      |    "num_txs" : 0,
      |    "total_txs" : 0,
      |    "last_block_id" : {
      |      "hash" : "Xy0fts7U0U/05+ZaEm1UEYLMZZCPdVPryOL3CcZ731o=",
      |      "parts" : {
      |        "total" : 1,
      |        "hash" : "4V7IbW3TuNvATdLe7t7fKJfNo3tLRg4NcOeLBG+7qos="
      |      }
      |    },
      |    "last_commit_hash" : "04496aa1162ce1dd08cf90d0a52c3b71798faf938b5cd48f72551268ceb0d963",
      |    "data_hash" : "",
      |    "validators_hash" : "aaf30643f81c4e3a2911e29db9a7a9ebb6712d3573031b0b119a5d233d531474",
      |    "next_validators_hash" : "aaf30643f81c4e3a2911e29db9a7a9ebb6712d3573031b0b119a5d233d531474",
      |    "consensus_hash" : "048091bc7ddc283f77bfbf91d73c44da58c3df8a9cbc867405d8b7f3daada22f",
      |    "app_hash" : "0a88111852095cae045340ea1f0b279944b2a756a213d9b50107d7489771e159",
      |    "last_results_hash" : "",
      |    "evidence_hash" : "",
      |    "proposer_address" : "c8da714f66a07aaf42ef3423d128287b13ef4b70"
      |  },
      |  "data" : {
      |    "txs" : [
      |    ]
      |  },
      |  "last_commit" : {
      |    "block_id" : {
      |      "hash" : "Xy0fts7U0U/05+ZaEm1UEYLMZZCPdVPryOL3CcZ731o=",
      |      "parts" : {
      |        "total" : 1,
      |        "hash" : "4V7IbW3TuNvATdLe7t7fKJfNo3tLRg4NcOeLBG+7qos="
      |      }
      |    },
      |    "precommits" : [
      |      {
      |        "type" : 2,
      |        "height" : "1",
      |        "block_id" : {
      |          "hash" : "Xy0fts7U0U/05+ZaEm1UEYLMZZCPdVPryOL3CcZ731o=",
      |          "parts" : {
      |            "total" : 1,
      |            "hash" : "4V7IbW3TuNvATdLe7t7fKJfNo3tLRg4NcOeLBG+7qos="
      |          }
      |        },
      |        "timestamp" : "2019-10-10T14:35:05.042734800Z",
      |        "validator_address" : "Wm76B3n8DBQ7Eg2v3f3t4o1rnsw=",
      |        "signature" : "GHyAyEwT5McADfsiUTgaDH14od1rq4l+vvukYapOz1/5oab0JZs9FrShVsqhZ/03wQ9lwJ54uLiLCTsLNP5hDw=="
      |      },
      |      {
      |        "type" : 2,
      |        "height" : "1",
      |        "block_id" : {
      |          "hash" : "Xy0fts7U0U/05+ZaEm1UEYLMZZCPdVPryOL3CcZ731o=",
      |          "parts" : {
      |            "total" : 1,
      |            "hash" : "4V7IbW3TuNvATdLe7t7fKJfNo3tLRg4NcOeLBG+7qos="
      |          }
      |        },
      |        "timestamp" : "2019-10-10T14:35:04.943960500Z",
      |        "validator_address" : "yNpxT2ageq9C7zQj0SgoexPvS3A=",
      |        "validator_index" : 1,
      |        "signature" : "uH2GmR8/EXC+7T3k+dXYi6LBBIAf7+amRxxD9VamCRiSjv8HKDRlizEy+0OjpxLQ5jJHxHqCkoNxEn4tONatDw=="
      |      }
      |    ]
      |  }
      |}
      |""".stripMargin

  val block3: String =
    """
      |{
      |  "header" : {
      |    "version" : {
      |      "block" : "10"
      |    },
      |    "chain_id" : "1",
      |    "height" : 3,
      |    "time" : "2019-10-10T14:35:07.788524100Z",
      |    "num_txs" : 1,
      |    "total_txs" : 1,
      |    "last_block_id" : {
      |      "hash" : "wq1w9w/3iEE6Evv40JC/kEoTWMvacmmjbRIBqZiV5+U=",
      |      "parts" : {
      |        "total" : 1,
      |        "hash" : "HYugWoY9aENhQo7l2AScW0uFRTgM0jg8ha5qH69KiDI="
      |      }
      |    },
      |    "last_commit_hash" : "3a618bab4ca87734da49eeee68ce47c0b43b81b340cf8679f02c73af6b3393b5",
      |    "data_hash" : "97a220c4fb3b78100ab07935717b1b41cbea511160e2dd2a79c83333c29f0559",
      |    "validators_hash" : "aaf30643f81c4e3a2911e29db9a7a9ebb6712d3573031b0b119a5d233d531474",
      |    "next_validators_hash" : "aaf30643f81c4e3a2911e29db9a7a9ebb6712d3573031b0b119a5d233d531474",
      |    "consensus_hash" : "048091bc7ddc283f77bfbf91d73c44da58c3df8a9cbc867405d8b7f3daada22f",
      |    "app_hash" : "0a88111852095cae045340ea1f0b279944b2a756a213d9b50107d7489771e159",
      |    "last_results_hash" : "",
      |    "evidence_hash" : "",
      |    "proposer_address" : "5a6efa0779fc0c143b120dafddfdede28d6b9ecc"
      |  },
      |  "data" : {
      |    "txs" : [
      |      "c2Vzc2lvbi8wCgAAAAAAAAAAAAA="
      |    ]
      |  },
      |  "last_commit" : {
      |    "block_id" : {
      |      "hash" : "wq1w9w/3iEE6Evv40JC/kEoTWMvacmmjbRIBqZiV5+U=",
      |      "parts" : {
      |        "total" : 1,
      |        "hash" : "HYugWoY9aENhQo7l2AScW0uFRTgM0jg8ha5qH69KiDI="
      |      }
      |    },
      |    "precommits" : [
      |      {
      |        "type" : 2,
      |        "height" : "2",
      |        "block_id" : {
      |          "hash" : "wq1w9w/3iEE6Evv40JC/kEoTWMvacmmjbRIBqZiV5+U=",
      |          "parts" : {
      |            "total" : 1,
      |            "hash" : "HYugWoY9aENhQo7l2AScW0uFRTgM0jg8ha5qH69KiDI="
      |          }
      |        },
      |        "timestamp" : "2019-10-10T14:35:07.788524100Z",
      |        "validator_address" : "Wm76B3n8DBQ7Eg2v3f3t4o1rnsw=",
      |        "signature" : "BnQX522+D2CFEk44PeLWXYb4I2I/09RHJ31+gpP5FEfvgjG0DpiiyoOWFb+hVTb4cw4XXcAYHxZJcm3qqLepDg=="
      |      },
      |      {
      |        "type" : 2,
      |        "height" : "2",
      |        "block_id" : {
      |          "hash" : "wq1w9w/3iEE6Evv40JC/kEoTWMvacmmjbRIBqZiV5+U=",
      |          "parts" : {
      |            "total" : 1,
      |            "hash" : "HYugWoY9aENhQo7l2AScW0uFRTgM0jg8ha5qH69KiDI="
      |          }
      |        },
      |        "timestamp" : "2019-10-10T14:35:07.893410100Z",
      |        "validator_address" : "yNpxT2ageq9C7zQj0SgoexPvS3A=",
      |        "validator_index" : 1,
      |        "signature" : "ugG8ddhuRxskHIopuhtkXembAwNB7aRE2pqNs7/i9HR9k0EaM3ZNd1zpthD7CBo9ddhhBE4gfowgQi8JC98rCw=="
      |      }
      |    ]
      |  }
      |}
      |""".stripMargin

  val block4: String =
    """
      |{
      |  "header" : {
      |    "version" : {
      |      "block" : "10"
      |    },
      |    "chain_id" : "1",
      |    "height" : 4,
      |    "time" : "2019-10-10T14:35:14.325228600Z",
      |    "num_txs" : 0,
      |    "total_txs" : 1,
      |    "last_block_id" : {
      |      "hash" : "4iogl5EOhrKaHVYdo0lpW6HIzno5yxyCxYX5A0W2EAk=",
      |      "parts" : {
      |        "total" : 1,
      |        "hash" : "c3+ExlKGMImDUS3k1YwppBJHsnx4exzNFPgOjGb478M="
      |      }
      |    },
      |    "last_commit_hash" : "3e631df0e8133f34e1d7fff97c4b9e193c56c746b80589fa1f756bab61860437",
      |    "data_hash" : "",
      |    "validators_hash" : "aaf30643f81c4e3a2911e29db9a7a9ebb6712d3573031b0b119a5d233d531474",
      |    "next_validators_hash" : "aaf30643f81c4e3a2911e29db9a7a9ebb6712d3573031b0b119a5d233d531474",
      |    "consensus_hash" : "048091bc7ddc283f77bfbf91d73c44da58c3df8a9cbc867405d8b7f3daada22f",
      |    "app_hash" : "0175ce0b3ff84153b8d9729f2b48908208c1dfa5070ae7aa6d2eb7485794a766",
      |    "last_results_hash" : "a68cd8ed2b5f99f531f9115e92d15dece9aba13988b4f5708199ae7782ca8288",
      |    "evidence_hash" : "",
      |    "proposer_address" : "c8da714f66a07aaf42ef3423d128287b13ef4b70"
      |  },
      |  "data" : {
      |    "txs" : [
      |    ]
      |  },
      |  "last_commit" : {
      |    "block_id" : {
      |      "hash" : "4iogl5EOhrKaHVYdo0lpW6HIzno5yxyCxYX5A0W2EAk=",
      |      "parts" : {
      |        "total" : 1,
      |        "hash" : "c3+ExlKGMImDUS3k1YwppBJHsnx4exzNFPgOjGb478M="
      |      }
      |    },
      |    "precommits" : [
      |      {
      |        "type" : 2,
      |        "height" : "3",
      |        "block_id" : {
      |          "hash" : "4iogl5EOhrKaHVYdo0lpW6HIzno5yxyCxYX5A0W2EAk=",
      |          "parts" : {
      |            "total" : 1,
      |            "hash" : "c3+ExlKGMImDUS3k1YwppBJHsnx4exzNFPgOjGb478M="
      |          }
      |        },
      |        "timestamp" : "2019-10-10T14:35:14.331336Z",
      |        "validator_address" : "Wm76B3n8DBQ7Eg2v3f3t4o1rnsw=",
      |        "signature" : "CYqvcDbYeRDoRjEgeglZhZw9kDO8YfXnSoU1bqcJc+77haF9byZbKB0yUCTPVPKA8MFO1P/MIBUwvZ5PtG4rCg=="
      |      },
      |      {
      |        "type" : 2,
      |        "height" : "3",
      |        "block_id" : {
      |          "hash" : "4iogl5EOhrKaHVYdo0lpW6HIzno5yxyCxYX5A0W2EAk=",
      |          "parts" : {
      |            "total" : 1,
      |            "hash" : "c3+ExlKGMImDUS3k1YwppBJHsnx4exzNFPgOjGb478M="
      |          }
      |        },
      |        "timestamp" : "2019-10-10T14:35:14.325228600Z",
      |        "validator_address" : "yNpxT2ageq9C7zQj0SgoexPvS3A=",
      |        "validator_index" : 1,
      |        "signature" : "n8iDNTHNJKVYjCasjs17I+KIbambsBt01v05T8/vRBPk+N06Z62T9QtFia+rlb1UsDID/zGVRI08lZQ+QEVhDg=="
      |      }
      |    ]
      |  }
      |}
      |""".stripMargin

  val block5: String =
    """
      |{
      |  "header" : {
      |    "version" : {
      |      "block" : "10"
      |    },
      |    "chain_id" : "1",
      |    "height" : 5,
      |    "time" : "2019-10-10T14:35:15.325228600Z",
      |    "num_txs" : 2,
      |    "total_txs" : 3,
      |    "last_block_id" : {
      |      "hash" : "BEV7fwSWPJqYSGvKUYs+JGoVHPdN6pWpMkqCh3ksofQ=",
      |      "parts" : {
      |        "total" : 1,
      |        "hash" : "jJehmRHQMefsAzvx1wTu+i1nSeZBJtZbHI6sz0msQgI="
      |      }
      |    },
      |    "last_commit_hash" : "412ed1d63e985ee29eb63be80bd89391ff70975541cdd6fa16977ec0b9685f36",
      |    "data_hash" : "b27f275d3a3cec5530014dbac40f7a10feb13bbc350d95b8734daf9bcead6f4d",
      |    "validators_hash" : "aaf30643f81c4e3a2911e29db9a7a9ebb6712d3573031b0b119a5d233d531474",
      |    "next_validators_hash" : "aaf30643f81c4e3a2911e29db9a7a9ebb6712d3573031b0b119a5d233d531474",
      |    "consensus_hash" : "048091bc7ddc283f77bfbf91d73c44da58c3df8a9cbc867405d8b7f3daada22f",
      |    "app_hash" : "0175ce0b3ff84153b8d9729f2b48908208c1dfa5070ae7aa6d2eb7485794a766",
      |    "last_results_hash" : "",
      |    "evidence_hash" : "",
      |    "proposer_address" : "5a6efa0779fc0c143b120dafddfdede28d6b9ecc"
      |  },
      |  "data" : {
      |    "txs" : [
      |      "c2Vzc2lvbi8xCgAAAAAAAAAAAAA=",
      |      "c2Vzc2lvbi8yCgAAAAAAAAAAAAA="
      |    ]
      |  },
      |  "last_commit" : {
      |    "block_id" : {
      |      "hash" : "BEV7fwSWPJqYSGvKUYs+JGoVHPdN6pWpMkqCh3ksofQ=",
      |      "parts" : {
      |        "total" : 1,
      |        "hash" : "jJehmRHQMefsAzvx1wTu+i1nSeZBJtZbHI6sz0msQgI="
      |      }
      |    },
      |    "precommits" : [
      |      {
      |        "type" : 2,
      |        "height" : "4",
      |        "block_id" : {
      |          "hash" : "BEV7fwSWPJqYSGvKUYs+JGoVHPdN6pWpMkqCh3ksofQ=",
      |          "parts" : {
      |            "total" : 1,
      |            "hash" : "jJehmRHQMefsAzvx1wTu+i1nSeZBJtZbHI6sz0msQgI="
      |          }
      |        },
      |        "timestamp" : "2019-10-10T14:35:15.325228600Z",
      |        "validator_address" : "Wm76B3n8DBQ7Eg2v3f3t4o1rnsw=",
      |        "signature" : "3MXehgPFAhDYQiBwPpz1cn0CNKbof193ut7Iui7DmC7AdFoizw/Pj/yQ0BpQWJ9bLBCHAgntJQRO9u/IpOyEBA=="
      |      },
      |      {
      |        "type" : 2,
      |        "height" : "4",
      |        "block_id" : {
      |          "hash" : "BEV7fwSWPJqYSGvKUYs+JGoVHPdN6pWpMkqCh3ksofQ=",
      |          "parts" : {
      |            "total" : 1,
      |            "hash" : "jJehmRHQMefsAzvx1wTu+i1nSeZBJtZbHI6sz0msQgI="
      |          }
      |        },
      |        "timestamp" : "2019-10-10T14:35:15.325228600Z",
      |        "validator_address" : "yNpxT2ageq9C7zQj0SgoexPvS3A=",
      |        "validator_index" : 1,
      |        "signature" : "mcw9wgnWA/oBZnUl6Wy1BPyjwPzdr0qyV0WL0dMJqIh9lkjDe3DSAeZaxSyxwFQwa5djwIdFRPSwGsfzQT95Bw=="
      |      }
      |    ]
      |  }
      |}
      |""".stripMargin

  val validators: Map[String, ByteVector] = Map(
    "5A6EFA0779FC0C143B120DAFDDFDEDE28D6B9ECC" ->
      ByteVector.fromBase64("83kIgtAQc8vcdnEqXfP+wVmi9VBVzk1KF259kuWxFdQ=").get,
    "C8DA714F66A07AAF42EF3423D128287B13EF4B70" ->
      ByteVector.fromBase64("CQR5oIPDZhAuCjikrl3aAn77AdNVDJAWNz2ODv+6mtU=").get
  )

  val blocks: Seq[Block] = {
    import io.circe.parser._
    import SimpleJsonCodecs.Decoders._

    for (block <- Seq(block1, block2, block3, block4, block5)) yield parse(block).flatMap(_.as[Block]).right.get
  }
}
